package com.lazytntutils.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        input.read(Entity.TAG_MOTION, Vec3.CODEC)
                .ifPresent(motion -> ((Entity) (Object) this).setDeltaMovement(motion));
    }
}
