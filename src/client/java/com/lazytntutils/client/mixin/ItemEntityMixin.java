package com.lazytntutils.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import com.lazytntutils.client.ClientItemStorage;
import com.lazytntutils.client.ClientSyncConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 用权威状态修正物品实体的客户端表现（本地修正，与 TntEntityMixin 平行）。
 *
 * <p>client 开（本地修正）：tick 开头取消本地模拟，直接把本地物品修正为权威状态 （防弱加载区物品自行下落/漂移）。 client 关：保留本地模拟；多人且有服务端下发时，仅在
 * tick 末尾把状态纠回服务端真值。 单人直读权威世界；多人使用网络下发（ClientItemStorage）。server 开关只决定服务端是否下发。
 */
@Environment(EnvType.CLIENT)
@Mixin(ItemEntity.class)
public class ItemEntityMixin {

  @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
  private void onTick(CallbackInfo ci) {
    ItemEntity self = (ItemEntity) (Object) this;
    if (!self.level().isClientSide()) return;
    if (ClientSyncConfig.itemClientSync) {
      // client 开（本地修正）：取消本地模拟并直接修正为权威状态（单人直读 / 多人网络下发）；无来源则保持不动。
      if (!applyDirect(self)) applyNetwork(self);
      ci.cancel();
    }
    // client 关：本地模拟照常跑，是否纠回由 TAIL 决定。
  }

  @Inject(method = "tick", at = @At("TAIL"))
  private void onTickTail(CallbackInfo ci) {
    ItemEntity self = (ItemEntity) (Object) this;
    if (!self.level().isClientSide()) return;
    if (ClientSyncConfig.itemClientSync) return; // client 开已在 HEAD 处理。
    // client 关：仅当多人且服务端仍在下发时纠回；单人 / 服务端未下发则为纯原版。
    applyNetwork(self);
  }

  /** 单人：直读集成服务端权威世界（零延迟，与 server 开关无关）。取到则修正本地并返回 true。 */
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

  /** 多人：套用服务端经网络下发的权威状态（仅专用服且服务端开关开启时才有数据）。 */
  private static void applyNetwork(ItemEntity self) {
    if (Minecraft.getInstance().getSingleplayerServer() != null) return; // 单人没有网络下发通道
    if (!ClientSyncConfig.itemServerSync) return; // 服务端已停止下发：不套用（防陈旧缓存）
    if (ClientSyncConfig.serverPaused) return; // tick sprint 期间服务端停发：套用会把物品纠回 sprint 前的位置
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
