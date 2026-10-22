package com.lazytntutils.client.mixin;

import com.lazytntutils.Projectiles;
import com.lazytntutils.client.ClientExperienceOrbStorage;
import com.lazytntutils.client.ClientFallingBlockStorage;
import com.lazytntutils.client.ClientProjectileStorage;
import com.lazytntutils.client.ClientSyncConfig;
import com.lazytntutils.client.MotionState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.class)
public class ClientEntityFreezeMixin {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void lazytntutils$freezeBeforeTick(Entity entity, CallbackInfo ci) {
        if (!shouldFreeze(entity)) return;
        entity.setOldPosAndRot();
        entity.tickCount++;
        if (!applyDirect(entity)) applyNetwork(entity);
        ci.cancel();
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void lazytntutils$correctAfterTick(Entity entity, CallbackInfo ci) {
        if (shouldFreeze(entity)) return;
        applyNetwork(entity);
    }

    private static boolean shouldFreeze(Entity entity) {
        if (Projectiles.isProjectile(entity)) return ClientSyncConfig.projectileClientSync;
        if (entity instanceof FallingBlockEntity) return ClientSyncConfig.fallingBlockClientSync;
        if (entity instanceof ExperienceOrb) return ClientSyncConfig.experienceOrbClientSync;
        return false;
    }

    private static boolean applyDirect(Entity self) {
        MinecraftServer integrated = Minecraft.getInstance().getSingleplayerServer();
        if (integrated == null) return false;
        ServerLevel serverWorld = integrated.getLevel(self.level().dimension());
        if (serverWorld == null) return false;
        Entity serverEntity = serverWorld.getEntity(self.getUUID());
        if (serverEntity == null) return false;
        override(
                self,
                serverEntity.getX(),
                serverEntity.getY(),
                serverEntity.getZ(),
                serverEntity.getDeltaMovement());
        return true;
    }

    private static void applyNetwork(Entity self) {
        if (Minecraft.getInstance().getSingleplayerServer() != null) return;
        if (ClientSyncConfig.serverPaused) return;
        MotionState state = networkState(self);
        if (state == null) return;
        override(
                self,
                state.x(),
                state.y(),
                state.z(),
                new Vec3(state.vx(), state.vy(), state.vz()));
    }

    private static MotionState networkState(Entity self) {
        if (Projectiles.isProjectile(self)) {
            return ClientSyncConfig.projectileServerSync
                    ? ClientProjectileStorage.get(self.getUUID())
                    : null;
        }
        if (self instanceof FallingBlockEntity) {
            return ClientSyncConfig.fallingBlockServerSync
                    ? ClientFallingBlockStorage.get(self.getUUID())
                    : null;
        }
        if (self instanceof ExperienceOrb) {
            return ClientSyncConfig.experienceOrbServerSync
                    ? ClientExperienceOrbStorage.get(self.getUUID())
                    : null;
        }
        return null;
    }

    private static void override(Entity self, double x, double y, double z, Vec3 velocity) {
        self.xo = x;
        self.yo = y;
        self.zo = z;
        self.setPos(x, y, z);
        self.setDeltaMovement(velocity);
    }
}
