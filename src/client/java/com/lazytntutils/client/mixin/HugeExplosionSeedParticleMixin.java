package com.lazytntutils.client.mixin;

import com.lazytntutils.client.ClientSyncConfig;
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

@Environment(EnvType.CLIENT)
@Mixin(HugeExplosionSeedParticle.class)
public class HugeExplosionSeedParticleMixin extends NoRenderParticle {

    private static final double PARTICLE_SIZE = 1.0;

    protected HugeExplosionSeedParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void lazytntutils$explodeAtCenter(CallbackInfo ci) {
        if (!ClientSyncConfig.simplifyExplosionParticles) return;
        ci.cancel();
        this.level.addParticle(
                ParticleTypes.EXPLOSION, this.x, this.y, this.z, PARTICLE_SIZE, 0.0, 0.0);
        this.remove();
    }
}
