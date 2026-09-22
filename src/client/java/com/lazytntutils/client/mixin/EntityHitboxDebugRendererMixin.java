package com.lazytntutils.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import com.lazytntutils.Projectiles;
import com.lazytntutils.TrackedEntities;
import com.lazytntutils.client.ClientSyncConfig;
import com.lazytntutils.client.MotionArrowMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为 TNT 与弹射物绘制速度矢量箭头（按 F3+B 开启碰撞箱时显示）。
 *
 * <p>原版的速度箭头只在「服务端侧碰撞箱」模式下绘制，依赖 getServerEntity()（仅单人可用）， 且受默认关闭的
 * SharedConstants.DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES 控制。 这里不启用那套服务端侧碰撞箱，而是直接用客户端自身的
 * getDeltaMovement() 绘制： 该值由服务端通过 setEntityMotion 持续同步，因此即使客户端不再本地 tick 运动 （本 mod 为修复弱加载实体消失 /
 * 自行运动而取消了客户端模拟），箭头依然准确，且多人下同样可用。
 *
 * <p>画法分别由 ClientSyncConfig.tntMotionArrow / projectileMotionArrow 决定（{@link MotionArrowMode}）：
 * 与原版一致的「线段 + 箭头」、只画线段、以及关闭。长距离高速弹道上箭头尖端会遮挡 末端位置，此时切到"仅线段"更清爽。
 *
 * <p>弹射物用青色、TNT 用原版的黄色，便于在同一屏里区分两类矢量。
 *
 * <p>另外还可按开关掐掉原版那根**蓝色朝向箭头**（{@code Gizmos.arrow(eyePosition, eyePosition + viewVector * 2,
 * -16776961)}）： 它对 TNT / 物品 / 弹射物 / 下落方块 / 经验球没有意义，开着只是给几百个实体各加一根蓝线。 见 {@link
 * ClientSyncConfig#showFacingArrow}。
 *
 * <p>注：serverSide 参数仅用于匹配目标方法签名。
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {

  /** 与原版服务端速度矢量箭头相同的颜色（黄），用于 TNT。 */
  private static final int TNT_ARROW_COLOR = 0xFFFFFF00;

  /** 弹射物矢量用青色，与 TNT 的黄色区分。 */
  private static final int PROJECTILE_ARROW_COLOR = 0xFF00FFFF;

  /**
   * 当前正在画碰撞箱的实体。
   *
   * <p>{@code showHitboxes} 里那根朝向箭头的调用点拿不到实体参数（{@code Gizmos.arrow} 是静态调用， @Redirect
   * 的处理器签名里只有三个实参），而"要不要掐掉"又必须按实体类别判断， 所以在 HEAD 记下实体供 Redirect 使用。{@code emitGizmos}
   * 是渲染线程上的单层循环、不会重入，这个字段不存在并发或嵌套问题。
   */
  @Unique private Entity lazytntutils$currentEntity;

  /**
   * 掐掉原版的蓝色朝向箭头。ordinal 0 = 朝向箭头（蓝 -16776961），ordinal 1 = 服务端侧速度矢量（黄，在 {@code isServerEntity}
   * 分支里）；26.1.2 与 26.2 的 {@code showHitboxes} 顺序一致。
   *
   * <p>返回 null 是安全的：调用处是语句表达式，返回值被丢弃。 不能改成"把朝向向量置零"来变相隐藏 —— {@code ArrowGizmo.emit} 会对 {@code end -
   * start} 做 {@code normalize()}，零长度向量会产生 NaN 顶点。
   */
  @Redirect(
      method = "showHitboxes",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/gizmos/Gizmos;arrow(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)Lnet/minecraft/gizmos/GizmoProperties;",
              ordinal = 0),
      require = 1)
  private GizmoProperties lazytntutils$hideFacingArrow(Vec3 start, Vec3 end, int color) {
    if (!ClientSyncConfig.showFacingArrow
        && TrackedEntities.isTracked(lazytntutils$currentEntity)) {
      return null;
    }
    return Gizmos.arrow(start, end, color);
  }

  @Inject(method = "showHitboxes", at = @At("HEAD"), require = 1)
  private void lazytntutils$rememberEntity(
      Entity entity, float partialTick, boolean serverSide, CallbackInfo ci) {
    lazytntutils$currentEntity = entity;
  }

  @Inject(method = "showHitboxes", at = @At("RETURN"), require = 1)
  private void lazytntutils$drawMotionArrow(
      Entity entity, float partialTick, boolean serverSide, CallbackInfo ci) {
    int color;
    MotionArrowMode mode;
    if (entity instanceof PrimedTnt) {
      color = TNT_ARROW_COLOR;
      mode = ClientSyncConfig.tntMotionArrow;
    } else if (Projectiles.isProjectile(entity)) {
      color = PROJECTILE_ARROW_COLOR;
      mode = ClientSyncConfig.projectileMotionArrow;
    } else {
      return;
    }
    if (mode == MotionArrowMode.OFF) return;

    Vec3 motion = entity.getDeltaMovement();
    if (motion.lengthSqr() < 1.0E-8) return;
    Vec3 start = entity.getPosition(partialTick);
    Vec3 end = start.add(motion);
    if (mode == MotionArrowMode.ARROW) {
      Gizmos.arrow(start, end, color);
    } else {
      Gizmos.line(start, end, color);
    }
  }
}
