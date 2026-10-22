package com.lazytntutils;

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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        ServerTickEvents.START_SERVER_TICK.register(TickSprintGuard::onTick);
        ServerLifecycleEvents.SERVER_STARTED.register(DistanceEnforcer::apply);
        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> {
                    TickSprintGuard.reset();
                    SyncConfig.resetDistance();
                });

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> {
                    ServerPlayNetworking.send(handler.getPlayer(), configPayload());
                    if (TickSprintGuard.isSprinting()) {
                        ServerPlayNetworking.send(
                                handler.getPlayer(), new SyncPauseS2CPayload(true));
                    }
                    DistanceEnforcer.apply(server);
                });

        ServerPlayNetworking.registerGlobalReceiver(
                SyncConfigC2SPayload.ID,
                (payload, context) -> {
                    context.player()
                            .level()
                            .getServer()
                            .execute(
                                    () -> {
                                        if (!applyServerFlag(payload.kind(), payload.value()))
                                            return;
                                        SyncConfig.save();
                                        broadcastConfig(context.player().level().getServer());
                                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(
                SyncDistanceC2SPayload.ID,
                (payload, context) -> {
                    context.player()
                            .level()
                            .getServer()
                            .execute(
                                    () -> {
                                        int value = SyncConfig.clampDistance(payload.value());
                                        if (payload.kind().equals("view"))
                                            SyncConfig.viewDistanceOverride = value;
                                        else if (payload.kind().equals("sim"))
                                            SyncConfig.simulationDistanceOverride = value;
                                        else return;
                                        MinecraftServer server =
                                                context.player().level().getServer();
                                        DistanceEnforcer.apply(server);
                                        broadcastConfig(server);
                                    });
                });
    }

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

    private static void broadcastConfig(MinecraftServer server) {
        if (server == null) return;
        SyncConfigS2CPayload payload = configPayload();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(p, payload);
        }
    }
}
