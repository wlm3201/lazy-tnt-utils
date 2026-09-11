package net.orbitalstrike.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.HugeExplosionSeedParticle;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让爆炸粒子精确显示在爆炸中心，而不是散布在周围。
 *
 * <p>原版行为（26.1.2）：爆炸时先生成一个 EXPLOSION_EMITTER，它是不渲染的"种子粒子" （HugeExplosionSeedParticle），由它在 tick()
 * 里喷出真正的 EXPLOSION 粒子： for (i = 0; i < 6; i++) 位置 = 中心 + (rand - rand) * 4.0 且 lifetime = 8，即连续 8
 * tick、每 tick 6 个，共 48 个粒子随机散布在中心 ±4 格内。 结果是完全无法据此判断 TNT 究竟在哪一格爆炸——对炮械设计是致命的。
 *
 * <p>这里在 tick() 开头取消原版逻辑，改为在种子粒子自身的坐标（即爆炸中心， 由服务端/爆炸包精确给出）生成唯一一个 EXPLOSION 粒子，速度 0，原地显示后消失。
 *
 * <p>无开关、默认生效：本 mod 的用途就是在弱加载 TNT 场景看清爆炸点，不提供退回原版的选项。
 *
 * <p>实现说明：这里 extends NoRenderParticle（目标类的父类）而不用 @Shadow。 因为 level / x / y / z 都声明在 Particle
 * 中，@Shadow 只在目标类自身查找、不查父类， 会抛 "was not located in the target class"；继承父类后这些字段就是普通继承成员，
 * 编译期可见、运行时直接访问目标实例，无需任何绑定。
 *
 * <p>与其它 mod（如 Tweakeroo 的"爆炸粒子简化"）的关系：本注入位于 HEAD 且 cancel， 原方法体（含其它 mod
 * 对循环次数/偏移常量的修改）整体不会执行，因此无论对方开关如何， 最终都只渲染我们这一个。不依赖、也不干涉 MaLiLib 等任何外部配置。
 */
@Environment(EnvType.CLIENT)
@Mixin(HugeExplosionSeedParticle.class)
public class HugeExplosionSeedParticleMixin extends NoRenderParticle {

  /**
   * EXPLOSION 粒子（HugeExplosionParticle）的构造里： quadSize = 2.0 * (1 - size * 0.5) 也就是说 size
   * 越大粒子越小，size = 2 时完全不可见。 原版传的是 age/lifetime（0→1），粒子从 2.0 缩到 1.0；这里固定用 1.0， 即原版动画的末态尺寸（约 1
   * 格宽），既能看清爆点又不过分遮挡。 想再调小就把这个值往 2.0 加（1.5 → 0.5 格）。
   */
  private static final double PARTICLE_SIZE = 1.0;

  protected HugeExplosionSeedParticleMixin(ClientLevel level, double x, double y, double z) {
    super(level, x, y, z);
  }

  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void lazytntutils$explodeAtCenter(CallbackInfo ci) {
    ci.cancel();
    this.level.addParticle(
        ParticleTypes.EXPLOSION, this.x, this.y, this.z, PARTICLE_SIZE, 0.0, 0.0);
    this.remove();
  }
}
