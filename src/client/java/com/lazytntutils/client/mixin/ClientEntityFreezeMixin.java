package com.lazytntutils.client.mixin;

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
import com.lazytntutils.Projectiles;
import com.lazytntutils.client.ClientExperienceOrbStorage;
import com.lazytntutils.client.ClientFallingBlockStorage;
import com.lazytntutils.client.ClientProjectileStorage;
import com.lazytntutils.client.ClientSyncConfig;
import com.lazytntutils.client.MotionState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 弹射物 / 下落方块 / 经验球的客户端表现修正（本地修正 / 纠回），与 TntEntityMixin、ItemEntityMixin 平行。
 *
 * <p>## 为什么注入 ClientLevel 而不是各个实体类
 *
 * <p>TNT 与物品各自只有一个类，注入 {@code PrimedTnt.tick()} / {@code ItemEntity.tick()} 即可。 弹射物却是十几个类：{@code
 * AbstractArrow.tick()} 里先跑完全部运动再在末尾 {@code super.tick()}， 因此注入 {@code Projectile.tick()}
 * 只能掐掉基类那一段、运动照旧； 逐个类注入则要在 Arrow / ThrowableProjectile / AbstractHurtingProjectile / ShulkerBullet
 * / LlamaSpit / FireworkRocketEntity / FishingHook …… 上各挂一遍，任一个子类漏掉就失效。
 *
 * <p>客户端实体 tick 的唯一入口是 {@code ClientLevel.tickNonPassenger(Entity)}：
 *
 * <pre>
 * entity.setOldPosAndRot();
 * entity.tickCount++;
 * Profiler.get().push(...);
 * entity.tick();                       // ← 各个实体自己的 tick
 * Profiler.get().pop();
 * </pre>
 *
 * <p>在其 HEAD 取消即可一次性覆盖所有弹射物 / 下落方块，且与"取消 tick()"完全等价 —— 上面那两步原版操作在这里手工补回来。 （不能改用
 * {@code @At("INVOKE" target = "Entity;tick()")} 取消：那时 Profiler 已经 push，取消会让 pop 不执行， profiler
 * 栈就此失衡。）
 *
 * <p>## 开关语义
 *
 * <p>client 开（本地修正）：取消本地模拟并把本地实体修正为权威状态（单人直读 / 多人网络下发）；取不到权威状态时保持不动（冻结）。 client 关：本地模拟照常跑，单人 /
 * 服务端未下发时为纯原版；多人且有下发时在 tick 末尾纠回服务端真值。 server 开关只决定服务端是否下发。
 */
@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.class)
public class ClientEntityFreezeMixin {

  @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
  private void lazytntutils$freezeBeforeTick(Entity entity, CallbackInfo ci) {
    if (!shouldFreeze(entity)) return;
    // 取消整个方法会连带跳过原版这两步，补回来以保持与"只取消 tick()"一致（xo 随后仍会被 override 覆盖）。
    entity.setOldPosAndRot();
    entity.tickCount++;
    if (!applyDirect(entity)) applyNetwork(entity);
    ci.cancel();
  }

  @Inject(method = "tickNonPassenger", at = @At("RETURN"))
  private void lazytntutils$correctAfterTick(Entity entity, CallbackInfo ci) {
    if (shouldFreeze(entity)) return; // client 开：已在 HEAD 冻结，不再重复处理。
    // client 关：仅当多人且服务端仍在下发时纠回；单人 / 服务端未下发则为纯原版。
    applyNetwork(entity);
  }

  /** 该实体所属的类别是否开启了本地修正；不属于本 mod 任何类别则返回 false（原样放行）。 */
  private static boolean shouldFreeze(Entity entity) {
    if (Projectiles.isProjectile(entity)) return ClientSyncConfig.projectileClientSync;
    if (entity instanceof FallingBlockEntity) return ClientSyncConfig.fallingBlockClientSync;
    if (entity instanceof ExperienceOrb) return ClientSyncConfig.experienceOrbClientSync;
    return false;
  }

  /** 单人：直读集成服务端权威世界（零延迟，与 server 开关无关）。取到则修正本地并返回 true。 */
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

  /** 多人：套用服务端经网络下发的权威状态（仅专用服且对应类别的 server 开关开启时才有数据）。 */
  private static void applyNetwork(Entity self) {
    if (Minecraft.getInstance().getSingleplayerServer() != null) return; // 单人没有网络下发通道
    if (ClientSyncConfig.serverPaused) return; // tick sprint 期间服务端停发：套用会把实体纠回 sprint 前的位置
    MotionState state = networkState(self);
    if (state == null) return;
    override(self, state.x(), state.y(), state.z(), new Vec3(state.vx(), state.vy(), state.vz()));
  }

  /** 按类别取网络缓存；类别未开启服务端下发时返回 null（防陈旧缓存）。 */
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
