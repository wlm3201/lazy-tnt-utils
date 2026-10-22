package com.lazytntutils.client.mixin;

import com.lazytntutils.client.ClientItemStorage;
import com.lazytntutils.client.ClientSyncConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide()) return;
        if (ClientSyncConfig.itemClientSync) {
            if (!applyDirect(self)) applyNetwork(self);
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide()) return;
        if (ClientSyncConfig.itemClientSync) return;
        applyNetwork(self);
    }

    private static boolean applyDirect(ItemEntity self) {
        MinecraftServer integrated = Minecraft.getInstance().getSingleplayerServer();
        if (integrated == null) return false;
        ServerLevel serverWorld = integrated.getLevel(self.level().dimension());
        if (serverWorld == null) return false;
        Entity e = serverWorld.getEntity(self.getUUID());
        if (!(e instanceof ItemEntity serverItem)) return false;
        override(
                self,
                serverItem.getX(),
                serverItem.getY(),
                serverItem.getZ(),
                serverItem.getDeltaMovement());
        return true;
    }

    private static void applyNetwork(ItemEntity self) {
        if (Minecraft.getInstance().getSingleplayerServer() != null) return;
        if (!ClientSyncConfig.itemServerSync) return;
        if (ClientSyncConfig.serverPaused) return;
        ClientItemStorage.ItemState s = ClientItemStorage.get(self.getUUID());
        if (s == null) return;
        override(self, s.x, s.y, s.z, new Vec3(s.vx, s.vy, s.vz));
    }

    private static void override(ItemEntity self, double x, double y, double z, Vec3 velocity) {
        self.xo = x;
        self.yo = y;
        self.zo = z;
        self.setPos(x, y, z);
        self.setDeltaMovement(velocity);
    }
}
