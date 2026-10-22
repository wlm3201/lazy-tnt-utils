package com.lazytntutils.client.mixin;

import com.lazytntutils.Projectiles;
import com.lazytntutils.TrackedEntities;
import com.lazytntutils.client.ClientSyncConfig;
import com.lazytntutils.client.MotionArrowMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {

    private static final int TNT_ARROW_COLOR = 0xFFFFFF00;

    private static final int PROJECTILE_ARROW_COLOR = 0xFF00FFFF;

    @Unique private Entity lazytntutils$currentEntity;

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
