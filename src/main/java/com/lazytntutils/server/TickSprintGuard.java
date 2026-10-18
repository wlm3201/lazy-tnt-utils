package com.lazytntutils.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.lazytntutils.LazyTNTUtils;
import com.lazytntutils.network.SyncPauseS2CPayload;

/**
 * 原版 /tick sprint 期间暂停实体同步。
 *
 * <p>判定直接采用原版的权威状态 MinecraftServer.tickRateManager().isSprinting()， 而不是自己用 tick 时长之类的启发式去猜： -
 * sprint 一执行就为 true，跑完或被中止即变 false，是天然的「开始/结束」翻转点； - /tick rate 提高刻速率不会改动 sprint 状态，因此不会被误伤； -
 * 服务器卡顿（tick 变慢）同样不会误判。
 *
 * <p>暂停只影响「这一刻发不发 / 套不套用」，不写 SyncConfig、不落盘、不改任何开关值， 因此 sprint 结束后行为完全回到 sprint 前，原本关闭同步的玩家不会被打开。
 */
public final class TickSprintGuard {

  private TickSprintGuard() {}

  /** 当前是否处于 sprint 中；lastAnnounced 是已告知客户端的上一状态。 */
  private static boolean sprinting = false;

  private static boolean lastAnnounced = false;

  /** 当前是否处于 sprint 暂停期：true 表示服务端已停发，客户端也不该套用缓存。 */
  public static boolean isSprinting() {
    return sprinting;
  }

  /**
   * 每个服务端 tick 开始时刷新状态，仅在翻转时通知客户端。 必须注册在 START_SERVER_TICK——两个追踪器在 END_SERVER_TICK 发包， 这样它们读到的永远是本
   * tick 的最新状态。
   */
  public static void onTick(MinecraftServer server) {
    if (server == null) return;
    sprinting = server.tickRateManager().isSprinting();
    if (sprinting == lastAnnounced) return; // 状态未翻转，不必打扰客户端
    lastAnnounced = sprinting;
    broadcast(server, sprinting);
  }

  /** 关服时复位，避免 sprint 中途退出导致下次进服残留暂停状态。 */
  public static void reset() {
    sprinting = false;
    lastAnnounced = false;
  }

  private static void broadcast(MinecraftServer server, boolean paused) {
    LazyTNTUtils.LOGGER.info(
        "[LazyTNTUtils] tick sprint{}，实体同步{}", paused ? "开始" : "结束", paused ? "暂停" : "恢复");
    SyncPauseS2CPayload payload = new SyncPauseS2CPayload(paused);
    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
      ServerPlayNetworking.send(p, payload);
    }
  }
}
