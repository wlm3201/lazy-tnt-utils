package com.lazytntutils.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import com.lazytntutils.SyncConfig;

/**
 * 把服务端的视距 / 模拟距离设置为配置值。
 *
 * <p>采用"事件驱动、需要时应用一次"而不是逐 tick 强制： 值与服务端状态一样是持久的，没人去改它就一直有效，没必要每 tick 兜圈。 触发时机见
 * LazyTNTUtils（服务端就绪、玩家加入、命令修改）。 因此：玩家自己改 carpet 规则或客户端 options 导致距离变化，本 mod 不会去抢回来—— 那是使用者自己的选择，本
 * mod 也从不读写 options.txt。
 *
 * <p>无论谁调用，都只在"当前值 != 目标值"时才真正下发。因为 PlayerList.setViewDistance / setSimulationDistance 内部会
 * broadcastAll(...) 给全体玩家发包，加这个判断可避免无谓广播。
 *
 * <p>走的是服务端 API，专用服上设完就是最终结果；单人（集成服务端）下视距会被原版每 tick 按视频设置拉回去， 由客户端侧 IntegratedServerMixin
 * 把覆盖值喂给原版那次同步来解决（模拟距离原版用的是自己的缓存字段，不受影响）。
 */
public final class DistanceEnforcer {

  private DistanceEnforcer() {}

  /** 需要时应用一次；值为 -1（不覆盖）的项直接跳过。 */
  public static void apply(MinecraftServer server) {
    if (server == null) return;
    PlayerList playerList = server.getPlayerList();
    if (playerList == null) return;

    int view = SyncConfig.viewDistanceOverride;
    if (view >= 0 && playerList.getViewDistance() != view) {
      playerList.setViewDistance(view);
    }

    int sim = SyncConfig.simulationDistanceOverride;
    if (sim >= 0 && playerList.getSimulationDistance() != sim) {
      playerList.setSimulationDistance(sim);
    }
  }
}
