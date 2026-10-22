package com.lazytntutils.client.mixin;

import com.lazytntutils.client.ClientSyncConfig;
import com.lazytntutils.client.ClientTntStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(PrimedTnt.class)
public class TntEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        if (!self.level().isClientSide()) return;
        if (ClientSyncConfig.tntClientSync) {
            if (!applyDirect(self)) applyNetwork(self);
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        if (!self.level().isClientSide()) return;
        if (ClientSyncConfig.tntClientSync) return;
        applyNetwork(self);
    }

    private static boolean applyDirect(PrimedTnt self) {
        MinecraftServer integrated = Minecraft.getInstance().getSingleplayerServer();
        if (integrated == null) return false;
        ServerLevel serverWorld = integrated.getLevel(self.level().dimension());
        if (serverWorld == null) return false;
        Entity e = serverWorld.getEntity(self.getUUID());
        if (!(e instanceof PrimedTnt serverTnt)) return false;
        override(
                self,
                serverTnt.getX(),
                serverTnt.getY(),
                serverTnt.getZ(),
                serverTnt.getDeltaMovement(),
                serverTnt.getFuse());
        return true;
    }

    private static void applyNetwork(PrimedTnt self) {
        if (Minecraft.getInstance().getSingleplayerServer() != null) return;
        if (!ClientSyncConfig.tntServerSync) return;
        if (ClientSyncConfig.serverPaused) return;
        ClientTntStorage.TntState s = ClientTntStorage.get(self.getUUID());
        if (s == null) return;
        override(self, s.x, s.y, s.z, new Vec3(s.vx, s.vy, s.vz), s.fuse);
    }

    private static void override(
            PrimedTnt self, double x, double y, double z, Vec3 velocity, int fuse) {
        self.xo = x;
        self.yo = y;
        self.zo = z;
        self.setPos(x, y, z);
        self.setDeltaMovement(velocity);
        self.setFuse(fuse);
    }
}
