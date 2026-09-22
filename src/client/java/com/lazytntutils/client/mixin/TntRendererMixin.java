package com.lazytntutils.client.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import com.lazytntutils.client.ClientSyncConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TNT 的渲染处理，两件事：头顶显示剩余**游戏刻**，以及处理白闪 / 缩放。
 *
 * <p>两者都通过 TntRenderState 完成，再由原版 EntityRenderer.submit 负责真正绘制—— 不自己调
 * submitNameTag。这样能直接复用原版已经算好的插值位置与 distanceToCameraSq， 标签会跟着实体的插值位置平滑移动；自行
 * translate(entity.getX()) 用的是未插值的 tick 坐标， 实体每 tick 才更新一次位置，标签就会一跳一跳地跟不稳（渲染 60fps、tick 只有 20Hz）。
 *
 * <p>## 刻数标签
 *
 * <p>直接设置 state.nameTag / nameTagAttachment，原版 submit 会在检测到 nameTag 非空时 提交名签。刻数取 entity.getFuse()
 * 的整数值，不受下面视觉修正的影响。
 *
 * <p>两处为"大量 TNT 时别拖帧"而做的取舍（见字段注释）：只画相机 64 格内的标签（与原版名签同一距离）， 同一刻数的 Component 全局复用，避免每颗 TNT 每帧都新建字符串
 * / Style / TextColor。
 *
 * <p>## 白闪与缩放
 *
 * <p>两者全由 TntRenderState.fuseRemainingInTicks 驱动： - 缩放：< 10 时 scale = 1 + (1 - fuse/10)⁴ × 0.3 -
 * 白闪：submitWhiteSolidBlock 的 (int)fuse / 5 % 2 == 0
 *
 * <p>该字段在 extractRenderState 里被算成 getFuse() - partialTicks + 1，其中的 partialTicks 每帧在 0~1
 * 波动。正常倒计时时引信在降，这点波动被趋势淹没；但弱加载区块引信恒定， 只剩 partialTicks 在抖，经 g⁴ 放大后就成了"变大变小"的搏动感，白闪也会来回切换。
 * 修正：只在引信确实没变的帧里改用定值 fuse + 1，于是冻结期间大小与配色完全锁定。
 *
 * <p>开关打开时（/lazytntutils tnt visual）则把该字段固定为 85：既 ≥ 10 跳过缩放分支， 又使 fuse / 5 = 17 为奇数（白闪判定
 * false），两个效果一并消失。
 */
@Environment(EnvType.CLIENT)
@Mixin(TntRenderer.class)
public class TntRendererMixin {

  private static final float NEUTRAL_FUSE = 85.0F;

  /** 名签挂在 TNT 头顶（TNT 包围盒高约 0.98，1.0 正好在其上方）。 */
  private static final Vec3 NAME_TAG_ATTACHMENT = new Vec3(0.0, 1.0, 0.0);

  /**
   * 标签的绘制距离上限（平方），取原版名签的 64 格（EntityRenderer.extractNameTags 的 nameTagDistance）。
   *
   * <p>原版在这里会把超距实体的 nameTag 置 null，而本 mixin 是**无条件**设值的（TAIL 注入在原版之后）， 等于把原版这道裁剪整个抵消掉了 —— 弱加载 TNT
   * 动辄几百颗且常常远在视距边缘， 于是几百个标签照样建 Component、照样进名签提交队列，却是屏幕上几个像素的糊团。 这里把那道裁剪补回来：超距就不设 nameTag，省掉建对象 +
   * 提交两步。
   */
  private static final double NAME_TAG_DISTANCE_SQ = 64.0 * 64.0;

  /**
   * 刻数 → Component 的缓存。标签内容只由引信决定（"80t" + 按刻数上色），同刻数的所有 TNT 完全共用同一个 Component（不可变，跨实体跨帧复用安全）。否则每颗
   * TNT 每帧都要拼字符串、建 Style 与 TextColor， N 颗 TNT 60fps 就是 60N 次分配。
   *
   * <p>容量上限只是兜底：正常引信 ≤ 80，但 /summon 能写出任意值，防止异常存档把表撑大。
   */
  private static final int MAX_LABEL_CACHE = 512;

  private static final Map<Integer, Component> LABEL_CACHE = new HashMap<>();

  /** 每个 TNT 上一次渲染时的引信，用于判断"引信没变 = 处于冻结"。弱引用，实体回收后自动清理。 */
  private static final Map<PrimedTnt, Integer> LAST_FUSE = new WeakHashMap<>();

  @Inject(method = "extractRenderState", at = @At("TAIL"))
  private void lazytntutils$applyTntVisuals(
      PrimedTnt entity, TntRenderState state, float partialTicks, CallbackInfo ci) {
    // 刻数用真实整数引信，先取，避免被下面的视觉修正改掉。
    int fuse = entity.getFuse();

    if (ClientSyncConfig.tntTickTimer && state.distanceToCameraSq <= NAME_TAG_DISTANCE_SQ) {
      state.nameTagAttachment = NAME_TAG_ATTACHMENT;
      state.nameTag = tickLabel(fuse);
    }

    if (ClientSyncConfig.tntNoFlashScale) {
      state.fuseRemainingInTicks = NEUTRAL_FUSE;
      return;
    }

    Integer last = LAST_FUSE.get(entity);
    if (last != null && last.intValue() == fuse) {
      state.fuseRemainingInTicks = fuse + 1.0F; // 冻结中：去掉 partialTicks 抖动
    }
    LAST_FUSE.put(entity, fuse);
  }

  private static Component tickLabel(int fuse) {
    Component cached = LABEL_CACHE.get(fuse);
    if (cached != null) return cached;
    if (LABEL_CACHE.size() >= MAX_LABEL_CACHE) LABEL_CACHE.clear();
    Component built =
        Component.literal(fuse + "t")
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorForFuse(fuse))));
    LABEL_CACHE.put(fuse, built);
    return built;
  }

  /** 临爆变红、进入最后一秒变橙，便于余光扫视。 */
  private static int colorForFuse(int fuse) {
    if (fuse < 20) return 0xFF0000;
    if (fuse < 40) return 0xFF8000;
    return 0xFFFFFF;
  }
}
