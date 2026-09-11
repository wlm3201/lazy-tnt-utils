package net.orbitalstrike.mixin;

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
import net.orbitalstrike.client.ClientSyncConfig;
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

  /** 每个 TNT 上一次渲染时的引信，用于判断"引信没变 = 处于冻结"。弱引用，实体回收后自动清理。 */
  private static final Map<PrimedTnt, Integer> LAST_FUSE = new WeakHashMap<>();

  @Inject(method = "extractRenderState", at = @At("TAIL"))
  private void lazytntutils$applyTntVisuals(
      PrimedTnt entity, TntRenderState state, float partialTicks, CallbackInfo ci) {
    // 刻数用真实整数引信，先取，避免被下面的视觉修正改掉。
    int fuse = entity.getFuse();

    if (ClientSyncConfig.tntTickTimer) {
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
    return Component.literal(fuse + "t")
        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorForFuse(fuse))));
  }

  /** 临爆变红、进入最后一秒变橙，便于余光扫视。 */
  private static int colorForFuse(int fuse) {
    if (fuse < 20) return 0xFF0000;
    if (fuse < 40) return 0xFF8000;
    return 0xFFFFFF;
  }
}
