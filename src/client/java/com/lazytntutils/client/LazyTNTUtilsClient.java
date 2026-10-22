package com.lazytntutils.client;

import com.lazytntutils.network.ExperienceOrbUpdatePayload;
import com.lazytntutils.network.FallingBlockUpdatePayload;
import com.lazytntutils.network.ItemUpdatePayload;
import com.lazytntutils.network.Networking;
import com.lazytntutils.network.ProjectileUpdatePayload;
import com.lazytntutils.network.SyncConfigC2SPayload;
import com.lazytntutils.network.SyncConfigS2CPayload;
import com.lazytntutils.network.SyncPauseS2CPayload;
import com.lazytntutils.network.TntUpdatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyTNTUtilsClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("lazytntutils");

    @Override
    public void onInitializeClient() {
        ClientSyncConfig.load();

        Networking.serverbound(SyncConfigC2SPayload.ID, SyncConfigC2SPayload.CODEC);
        Networking.clientbound(SyncConfigS2CPayload.ID, SyncConfigS2CPayload.CODEC);
        Networking.clientbound(SyncPauseS2CPayload.ID, SyncPauseS2CPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(
                TntUpdatePayload.ID,
                (payload, context) ->
                        context.client()
                                .execute(() -> ClientTntStorage.updateAll(payload.entries())));
        ClientPlayNetworking.registerGlobalReceiver(
                ItemUpdatePayload.ID,
                (payload, context) ->
                        context.client()
                                .execute(() -> ClientItemStorage.updateAll(payload.entries())));
        ClientPlayNetworking.registerGlobalReceiver(
                ProjectileUpdatePayload.ID,
                (payload, context) ->
                        context.client()
                                .execute(
                                        () ->
                                                ClientProjectileStorage.updateAll(
                                                        payload.entries())));
        ClientPlayNetworking.registerGlobalReceiver(
                FallingBlockUpdatePayload.ID,
                (payload, context) ->
                        context.client()
                                .execute(
                                        () ->
                                                ClientFallingBlockStorage.updateAll(
                                                        payload.entries())));
        ClientPlayNetworking.registerGlobalReceiver(
                ExperienceOrbUpdatePayload.ID,
                (payload, context) ->
                        context.client()
                                .execute(
                                        () ->
                                                ClientExperienceOrbStorage.updateAll(
                                                        payload.entries())));
        ClientPlayNetworking.registerGlobalReceiver(
                SyncConfigS2CPayload.ID,
                (payload, context) ->
                        context.client()
                                .execute(
                                        () -> {
                                            ClientSyncConfig.tntServerSync =
                                                    payload.tntServerSync();
                                            ClientSyncConfig.itemServerSync =
                                                    payload.itemServerSync();
                                            ClientSyncConfig.projectileServerSync =
                                                    payload.projectileServerSync();
                                            ClientSyncConfig.fallingBlockServerSync =
                                                    payload.fallingBlockServerSync();
                                            ClientSyncConfig.experienceOrbServerSync =
                                                    payload.experienceOrbServerSync();
                                            ClientSyncConfig.viewDistanceOverride =
                                                    payload.viewDistanceOverride();
                                            ClientSyncConfig.simulationDistanceOverride =
                                                    payload.simulationDistanceOverride();
                                        }));

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPauseS2CPayload.ID,
                (payload, context) ->
                        context.client()
                                .execute(
                                        () -> {
                                            ClientSyncConfig.serverPaused = payload.paused();
                                            if (payload.paused()) {
                                                ClientTntStorage.clear();
                                                ClientItemStorage.clear();
                                                ClientProjectileStorage.clear();
                                                ClientFallingBlockStorage.clear();
                                                ClientExperienceOrbStorage.clear();
                                            }
                                        }));

        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    ClientTntStorage.clear();
                    ClientItemStorage.clear();
                    ClientProjectileStorage.clear();
                    ClientFallingBlockStorage.clear();
                    ClientExperienceOrbStorage.clear();
                    ClientSyncConfig.reset();
                });
    }
}
