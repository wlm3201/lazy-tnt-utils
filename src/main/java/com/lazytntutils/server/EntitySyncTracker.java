package com.lazytntutils.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;

public abstract class EntitySyncTracker<E extends Entity> {

    private static final double SYNC_RADIUS = 160.0;

    private static final double SYNC_RADIUS_SQ = SYNC_RADIUS * SYNC_RADIUS;

    protected abstract boolean isEnabled();

    protected abstract EntityTypeTest<Entity, E> typeTest();

    protected Predicate<E> filter() {
        return entity -> true;
    }

    protected abstract CustomPacketPayload buildPayload(List<E> entities);

    public void onTick(MinecraftServer server) {
        if (!isEnabled()) return;
        if (TickSprintGuard.isSprinting()) return;

        Map<ServerLevel, List<E>> entitiesByLevel = new HashMap<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<E> all =
                    entitiesByLevel.computeIfAbsent((ServerLevel) player.level(), this::queryAll);

            double px = player.getX(), py = player.getY(), pz = player.getZ();
            List<E> nearby = new ArrayList<>();
            for (E entity : all) {
                if (entity.distanceToSqr(px, py, pz) <= SYNC_RADIUS_SQ) {
                    nearby.add(entity);
                }
            }

            if (nearby.isEmpty()) continue;
            ServerPlayNetworking.send(player, buildPayload(nearby));
        }
    }

    private List<E> queryAll(ServerLevel level) {
        List<E> result = new ArrayList<>();
        level.getEntities(typeTest(), filter(), result);
        return result;
    }
}
