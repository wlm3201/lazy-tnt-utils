package com.lazytntutils;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.lazytntutils.network.ExperienceOrbUpdatePayload;
import com.lazytntutils.network.FallingBlockUpdatePayload;
import com.lazytntutils.network.ItemUpdatePayload;
import com.lazytntutils.network.Networking;
import com.lazytntutils.network.ProjectileUpdatePayload;
import com.lazytntutils.network.SyncConfigC2SPayload;
import com.lazytntutils.network.SyncConfigS2CPayload;
import com.lazytntutils.network.SyncDistanceC2SPayload;
import com.lazytntutils.network.SyncPauseS2CPayload;
import com.lazytntutils.network.TntUpdatePayload;
import com.lazytntutils.server.DistanceEnforcer;
import com.lazytntutils.server.ServerExperienceOrbTracker;
import com.lazytntutils.server.ServerFallingBlockTracker;
import com.lazytntutils.server.ServerItemTracker;
import com.lazytntutils.server.ServerProjectileTracker;
import com.lazytntutils.server.ServerTntTracker;
import com.lazytntutils.server.TickSprintGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务端与集成服务端入口。 注册 TNT/物品 同步包与服务端开关同步包的编解码，启动服务端追踪器， 并在玩家加入时下发当前服务端开关、接收客户端的开关修改请求。
 *
 * <p>服务端不提供命令：所有开关都由客户端统一命令（见 LazyTNTUtilsClient）经 SyncConfigC2SPayload 发过来落地，本类只“接收 + 持久化 +
 * 广播”。这样单人不与服务端命令同名冲突， 客户端命令的 client/server 补全都能正常显示。专用服服主想改默认值可直接编辑
 * config/lazytntutils.properties，或进游戏用客户端命令。
 */
public class LazyTNTUtils implements ModInitializer {

  public static final String MOD_ID = "lazytntutils";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    SyncConfig.load();

    Networking.clientbound(TntUpdatePayload.ID, TntUpdatePayload.CODEC);
    Networking.clientbound(ItemUpdatePayload.ID, ItemUpdatePayload.CODEC);
    Networking.clientbound(ProjectileUpdatePayload.ID, ProjectileUpdatePayload.CODEC);
    Networking.clientbound(FallingBlockUpdatePayload.ID, FallingBlockUpdatePayload.CODEC);
    Networking.clientbound(ExperienceOrbUpdatePayload.ID, ExperienceOrbUpdatePayload.CODEC);
    Networking.clientbound(SyncConfigS2CPayload.ID, SyncConfigS2CPayload.CODEC);
    Networking.clientbound(SyncPauseS2CPayload.ID, SyncPauseS2CPayload.CODEC);
    Networking.serverbound(SyncConfigC2SPayload.ID, SyncConfigC2SPayload.CODEC);
    Networking.serverbound(SyncDistanceC2SPayload.ID, SyncDistanceC2SPayload.CODEC);

    ServerTntTracker.register();
    ServerItemTracker.register();
    ServerProjectileTracker.register();
    ServerFallingBlockTracker.register();
    ServerExperienceOrbTracker.register();

    // tick sprint 状态刷新跑在 START_SERVER_TICK：两个追踪器在 END_SERVER_TICK 发包，
    // 这样它们读到的永远是本 tick 最新的暂停状态。
    ServerTickEvents.START_SERVER_TICK.register(TickSprintGuard::onTick);
    // 服务端就绪后应用一次当前的距离覆盖值（此时 PlayerList 才可用）。
    ServerLifecycleEvents.SERVER_STARTED.register(DistanceEnforcer::apply);
    // 关服时复位进程内状态：sprint 暂停标记（避免下次进服残留暂停状态）与距离覆盖值。
    // 后者对单人尤其必要：集成服务端与游戏同进程，覆盖值是静态字段、会跟着跨存档存活，
    // 不在关服时清掉就会出现"换存档仍保留、退游戏才失效"。见 SyncConfig 距离字段。
    ServerLifecycleEvents.SERVER_STOPPED.register(
        server -> {
          TickSprintGuard.reset();
          SyncConfig.resetDistance();
        });

    // 玩家加入时下发当前服务端开关；若此刻正处于 sprint 暂停期，一并告知。
    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> {
          ServerPlayNetworking.send(handler.getPlayer(), configPayload());
          if (TickSprintGuard.isSprinting()) {
            ServerPlayNetworking.send(handler.getPlayer(), new SyncPauseS2CPayload(true));
          }
          // 单人下玩家的客户端设置会随加入同步进来并改掉服务端距离，这里再校正一次。
          DistanceEnforcer.apply(server);
        });

    // 接收客户端发来的服务端开关修改请求。
    ServerPlayNetworking.registerGlobalReceiver(
        SyncConfigC2SPayload.ID,
        (payload, context) -> {
          context
              .player()
              .level()
              .getServer()
              .execute(
                  () -> {
                    if (!applyServerFlag(payload.kind(), payload.value())) return;
                    SyncConfig.save();
                    broadcastConfig(context.player().level().getServer());
                  });
        });

    // 接收客户端发来的视距 / 模拟距离覆盖请求。
    ServerPlayNetworking.registerGlobalReceiver(
        SyncDistanceC2SPayload.ID,
        (payload, context) -> {
          context
              .player()
              .level()
              .getServer()
              .execute(
                  () -> {
                    int value = SyncConfig.clampDistance(payload.value());
                    if (payload.kind().equals("view")) SyncConfig.viewDistanceOverride = value;
                    else if (payload.kind().equals("sim"))
                      SyncConfig.simulationDistanceOverride = value;
                    else return;
                    MinecraftServer server = context.player().level().getServer();
                    // 距离不做持久化，只改内存值 + 立即应用 + 广播镜像。
                    DistanceEnforcer.apply(server);
                    broadcastConfig(server);
                  });
        });
  }

  /** 按类别名写服务端下发开关；类别名未知则不改动任何值（返回 false），避免误伤。 */
  private static boolean applyServerFlag(String kind, boolean value) {
    switch (kind) {
      case "tnt" -> SyncConfig.tntServerSync = value;
      case "item" -> SyncConfig.itemServerSync = value;
      case "projectile" -> SyncConfig.projectileServerSync = value;
      case "fallingblock" -> SyncConfig.fallingBlockServerSync = value;
      case "xp" -> SyncConfig.experienceOrbServerSync = value;
      default -> {
        return false;
      }
    }
    return true;
  }

  private static SyncConfigS2CPayload configPayload() {
    return new SyncConfigS2CPayload(
        SyncConfig.tntServerSync,
        SyncConfig.itemServerSync,
        SyncConfig.projectileServerSync,
        SyncConfig.fallingBlockServerSync,
        SyncConfig.experienceOrbServerSync,
        SyncConfig.viewDistanceOverride,
        SyncConfig.simulationDistanceOverride);
  }

  /** 向所有在线客户端广播当前服务端开关与距离覆盖值。 */
  private static void broadcastConfig(MinecraftServer server) {
    if (server == null) return;
    SyncConfigS2CPayload payload = configPayload();
    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
      ServerPlayNetworking.send(p, payload);
    }
  }
}
