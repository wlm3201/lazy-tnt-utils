package net.orbitalstrike.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import net.orbitalstrike.client.ClientSyncConfig;
import net.orbitalstrike.client.MotionArrowMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为 TNT 绘制速度矢量箭头（按 F3+B 开启碰撞箱时显示）。
 *
 * <p>原版的速度箭头只在「服务端侧碰撞箱」模式下绘制，依赖 getServerEntity()（仅单人可用）， 且受默认关闭的
 * SharedConstants.DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES 控制。 这里不启用那套服务端侧碰撞箱，而是直接用客户端自身的
 * getDeltaMovement() 绘制： 该值由服务端通过 setEntityMotion 持续同步，因此即使客户端不再本地 tick 运动 （本 mod 为修复弱加载 TNT
 * 消失而取消了客户端模拟），箭头依然准确，且多人下同样可用。
 *
 * <p>画法由 ClientSyncConfig.tntMotionArrow 决定（{@link MotionArrowMode}）：与原版一致的「线段 + 箭头」、
 * 只画线段、以及关闭。长距离高速弹道上箭头尖端会遮挡 末端位置，此时切到"仅线段"更清爽。
 *
 * <p>注：serverSide 参数仅用于匹配目标方法签名。
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {

  /** 与原版服务端速度矢量箭头相同的颜色（黄）。 */
  private static final int MOTION_ARROW_COLOR = 0xFFFFFF00;

  @Inject(method = "showHitboxes", at = @At("RETURN"), require = 1)
  private void lazytntutils$drawTntMotionArrow(
      Entity entity, float partialTick, boolean serverSide, CallbackInfo ci) {
    if (!(entity instanceof PrimedTnt)) return;
    MotionArrowMode mode = ClientSyncConfig.tntMotionArrow;
    if (mode == MotionArrowMode.OFF) return;

    Vec3 motion = entity.getDeltaMovement();
    if (motion.lengthSqr() < 1.0E-8) return;
    Vec3 start = entity.getPosition(partialTick);
    Vec3 end = start.add(motion);
    if (mode == MotionArrowMode.ARROW) {
      Gizmos.arrow(start, end, MOTION_ARROW_COLOR);
    } else {
      Gizmos.line(start, end, MOTION_ARROW_COLOR);
    }
  }
}
