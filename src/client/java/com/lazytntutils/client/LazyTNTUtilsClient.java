package com.lazytntutils.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.lazytntutils.network.ExperienceOrbUpdatePayload;
import com.lazytntutils.network.FallingBlockUpdatePayload;
import com.lazytntutils.network.ItemUpdatePayload;
import com.lazytntutils.network.Networking;
import com.lazytntutils.network.ProjectileUpdatePayload;
import com.lazytntutils.network.SyncConfigC2SPayload;
import com.lazytntutils.network.SyncConfigS2CPayload;
import com.lazytntutils.network.SyncPauseS2CPayload;
import com.lazytntutils.network.TntUpdatePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端入口：接收服务端下发的实体权威状态，缓存到对应 Storage； 接收服务端下发的开关镜像与 sprint 暂停通知；断连时清空缓存并复位镜像。
 *
 * <p>本类**不负责配置**：所有开关都在 {@link ConfigScreen}（YACL）里，经 ModMenu 的"配置"按钮打开。 早先的 {@code
 * /lazytntutils} 客户端命令已移除——命令能表达的，界面都能表达，而命令却有"改一项刷一堆回显、 参数格式要背、多人下改错无提示"的问题。开关的读写逻辑见
 * ClientSyncConfig 与 ConfigScreen。
 */
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
            context.client().execute(() -> ClientTntStorage.updateAll(payload.entries())));
    ClientPlayNetworking.registerGlobalReceiver(
        ItemUpdatePayload.ID,
        (payload, context) ->
            context.client().execute(() -> ClientItemStorage.updateAll(payload.entries())));
    ClientPlayNetworking.registerGlobalReceiver(
        ProjectileUpdatePayload.ID,
        (payload, context) ->
            context.client().execute(() -> ClientProjectileStorage.updateAll(payload.entries())));
    ClientPlayNetworking.registerGlobalReceiver(
        FallingBlockUpdatePayload.ID,
        (payload, context) ->
            context.client().execute(() -> ClientFallingBlockStorage.updateAll(payload.entries())));
    ClientPlayNetworking.registerGlobalReceiver(
        ExperienceOrbUpdatePayload.ID,
        (payload, context) ->
            context
                .client()
                .execute(() -> ClientExperienceOrbStorage.updateAll(payload.entries())));
    // 服务端下发的开关镜像：写入 ClientSyncConfig 供查询显示；是否本地修正只看本机 client 开关。
    ClientPlayNetworking.registerGlobalReceiver(
        SyncConfigS2CPayload.ID,
        (payload, context) ->
            context
                .client()
                .execute(
                    () -> {
                      ClientSyncConfig.tntServerSync = payload.tntServerSync();
                      ClientSyncConfig.itemServerSync = payload.itemServerSync();
                      ClientSyncConfig.projectileServerSync = payload.projectileServerSync();
                      ClientSyncConfig.fallingBlockServerSync = payload.fallingBlockServerSync();
                      ClientSyncConfig.experienceOrbServerSync = payload.experienceOrbServerSync();
                      ClientSyncConfig.viewDistanceOverride = payload.viewDistanceOverride();
                      ClientSyncConfig.simulationDistanceOverride =
                          payload.simulationDistanceOverride();
                    }));

    // tick sprint 期间服务端停发：客户端同步停止套用并丢弃缓存，避免拿陈旧数据把实体纠回旧位置。
    ClientPlayNetworking.registerGlobalReceiver(
        SyncPauseS2CPayload.ID,
        (payload, context) ->
            context
                .client()
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
