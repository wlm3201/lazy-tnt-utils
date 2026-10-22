package com.lazytntutils.server;

import com.lazytntutils.LazyTNTUtils;
import com.lazytntutils.network.SyncPauseS2CPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TickSprintGuard {

    private TickSprintGuard() {}

    private static boolean sprinting = false;

    private static boolean lastAnnounced = false;

    public static boolean isSprinting() {
        return sprinting;
    }

    public static void onTick(MinecraftServer server) {
        if (server == null) return;
        sprinting = server.tickRateManager().isSprinting();
        if (sprinting == lastAnnounced) return;
        lastAnnounced = sprinting;
        broadcast(server, sprinting);
    }

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
