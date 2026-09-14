package net.orbitalstrike.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 解除原版 {@code Entity.load} 对 Motion 的 ±10 限制。
 *
 * <p>原版读 NBT 时逐分量做 {@code Math.abs(v) > 10.0 ? 0.0 : v} —— 注意是**直接置零**而不是截断到 10：
 *
 * <pre>
 * this.setDeltaMovement(Math.abs(motion.x) &gt; 10.0 ? 0.0 : motion.x, ... y, ... z);
 * </pre>
 *
 * <p>于是「/summon 带 Motion NBT」和「/data set Motion」只要任一轴超过 10，那一轴就整体归零（entity.load 是这两条路径的共同入口： summon
 * 直接 load，data set 则是取出实体 NBT 改完再 load 回去）。 生电调试高速 TNT 很容易撞上，且表现得很像"值被忽略"。
 *
 * <p>做法：原版那次 {@code setDeltaMovement} 之后，用 NBT 里的原始值再写一次，把限制覆盖掉。相比重定向 {@code Math.abs}
 * 那种绕开判据的写法，这保留了原版其余逻辑；两版（26.1.2 / 26.2）的 load 内都只有这一处 {@code setDeltaMovement(DDD)}
 * 调用，注入点是唯一的（require = 1，版本变动会立刻报错而不是静默失效）。
 *
 * <p>副作用：这是原版的安全阀，解除后超高速实体会按真实速度 collisions / 穿帧推进，属于使用者自己的选择。
 */
@Mixin(Entity.class)
public class MotionCapMixin {

  @Inject(
      method = "load(Lnet/minecraft/world/level/storage/ValueInput;)V",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V",
              shift = At.Shift.AFTER,
              ordinal = 0),
      require = 1)
  private void lazytntutils$restoreRawMotion(ValueInput input, CallbackInfo ci) {
    input
        .read(Entity.TAG_MOTION, Vec3.CODEC)
        .ifPresent(motion -> ((Entity) (Object) this).setDeltaMovement(motion));
  }
}
