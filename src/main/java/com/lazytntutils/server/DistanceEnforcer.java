package com.lazytntutils.server;

import com.lazytntutils.SyncConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

public final class DistanceEnforcer {

    private DistanceEnforcer() {}

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
