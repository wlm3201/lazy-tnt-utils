package net.orbitalstrike;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.orbitalstrike.client.ClientItemStorage;
import net.orbitalstrike.client.ClientSyncConfig;
import net.orbitalstrike.client.ClientTntStorage;
import net.orbitalstrike.network.ItemUpdatePayload;
import net.orbitalstrike.network.Networking;
import net.orbitalstrike.network.SyncConfigC2SPayload;
import net.orbitalstrike.network.SyncConfigS2CPayload;
import net.orbitalstrike.network.SyncDistanceC2SPayload;
import net.orbitalstrike.network.SyncPauseS2CPayload;
import net.orbitalstrike.network.TntUpdatePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端入口：接收服务端下发的 TNT/物品 权威状态，缓存到对应 Storage； 注册客户端统一命令（同时管理“本地修正”与“服务端下发”两类开关），
 * 并接收服务端下发的开关镜像以在查询时显示。断连时清空实体状态并复位服务端开关镜像。
 */
public class LazyTNTutilsClient implements ClientModInitializer {

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
                      }
                    }));

    ClientPlayConnectionEvents.DISCONNECT.register(
        (handler, client) -> {
          ClientTntStorage.clear();
          ClientItemStorage.clear();
          ClientSyncConfig.reset();
        });

    registerClientCommand();
  }

  /**
   * 注册客户端统一命令，同时管理两类开关： /lazytntutils # 查询全部（client + server） /lazytntutils <tnt|item> # 查询某类型的
   * client + server /lazytntutils <tnt|item> client [true|false] # 设置“本地修正”：开 =
   * 客户端不再本地模拟、按服务端权威修正显示（持久化）；关 = 退回本地模拟 /lazytntutils <tnt|item> server [true|false] #
   * 设置“服务端是否下发”（发包给服务端，由服务端落地；多人下决定有无权威数据） client 开关在本地即时生效并写入客户端配置；server 开关经 SyncConfigC2SPayload
   * 交给服务端， 服务端改完会广播新的镜像回来，因此所有提示与状态始终一致。
   */
  private void registerClientCommand() {
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess) -> {
          LiteralArgumentBuilder<FabricClientCommandSource> root =
              LiteralArgumentBuilder.literal("lazytntutils");
          root.executes(
              ctx -> {
                sendAllStatus(ctx);
                return 1;
              });

          for (String type : new String[] {"tnt", "item"}) {
            LiteralArgumentBuilder<FabricClientCommandSource> typeNode =
                LiteralArgumentBuilder.literal(type);
            typeNode.executes(
                ctx -> {
                  sendOneStatus(ctx, type);
                  return 1;
                });
            for (String side : new String[] {"client", "server"}) {
              LiteralArgumentBuilder<FabricClientCommandSource> sideNode =
                  LiteralArgumentBuilder.literal(side);
              sideNode.executes(
                  ctx -> {
                    sendOneStatus(ctx, type);
                    return 1;
                  });
              sideNode.then(
                  RequiredArgumentBuilder.<FabricClientCommandSource, Boolean>argument(
                          "enabled", BoolArgumentType.bool())
                      .executes(
                          ctx -> {
                            boolean on = BoolArgumentType.getBool(ctx, "enabled");
                            if (side.equals("client")) {
                              setClientFlag(type, on);
                              ClientSyncConfig.save();
                            } else {
                              // 本地乐观更新镜像，并请求服务端落地（服务端会广播回最新值）。
                              setServerFlag(type, on);
                              ClientPlayNetworking.send(new SyncConfigC2SPayload(type, on));
                            }
                            sendOneStatus(ctx, type);
                            return 1;
                          }));
              typeNode.then(sideNode);
            }
            // TNT 的白闪 / 缩放是纯客户端渲染，直接在本机开关与持久化。
            if (type.equals("tnt")) {
              LiteralArgumentBuilder<FabricClientCommandSource> visualNode =
                  LiteralArgumentBuilder.literal("visual");
              visualNode.executes(
                  ctx -> {
                    sendVisualStatus(ctx);
                    return 1;
                  });
              visualNode.then(
                  RequiredArgumentBuilder.<FabricClientCommandSource, Boolean>argument(
                          "enabled", BoolArgumentType.bool())
                      .executes(
                          ctx -> {
                            ClientSyncConfig.tntNoFlashScale =
                                BoolArgumentType.getBool(ctx, "enabled");
                            ClientSyncConfig.save();
                            sendVisualStatus(ctx);
                            return 1;
                          }));
              typeNode.then(visualNode);

              // TNT 头顶刻数倒计时（纯客户端渲染）。
              LiteralArgumentBuilder<FabricClientCommandSource> timerNode =
                  LiteralArgumentBuilder.literal("timer");
              timerNode.executes(
                  ctx -> {
                    sendTimerStatus(ctx);
                    return 1;
                  });
              timerNode.then(
                  RequiredArgumentBuilder.<FabricClientCommandSource, Boolean>argument(
                          "enabled", BoolArgumentType.bool())
                      .executes(
                          ctx -> {
                            ClientSyncConfig.tntTickTimer =
                                BoolArgumentType.getBool(ctx, "enabled");
                            ClientSyncConfig.save();
                            sendTimerStatus(ctx);
                            return 1;
                          }));
              typeNode.then(timerNode);
            }
            root.then(typeNode);
          }

          // 渲染距离 / 模拟距离：值直接是区块数，0 与 1 都是真实值（-1 或 default = 不覆盖）。
          // 设置经 SyncDistanceC2SPayload 交给服务端落地：只改运行期内存值（不落盘），由服务端在事件点
          // （开服 / 玩家加入 / 命令修改）应用一次。重启服务端或单人下退出存档即失效，见 SyncConfig 距离字段。
          for (String kind : new String[] {"view", "sim"}) {
            LiteralArgumentBuilder<FabricClientCommandSource> kindNode =
                LiteralArgumentBuilder.literal(kind);
            kindNode.executes(
                ctx -> {
                  sendDistanceStatus(ctx, kind);
                  return 1;
                });
            kindNode.then(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("default")
                    .executes(
                        ctx -> {
                          applyDistance(kind, -1);
                          sendDistanceStatus(ctx, kind);
                          return 1;
                        }));
            kindNode.then(
                RequiredArgumentBuilder.<FabricClientCommandSource, Integer>argument(
                        "distance", IntegerArgumentType.integer(-1, SyncConfig.MAX_DISTANCE))
                    .executes(
                        ctx -> {
                          applyDistance(kind, IntegerArgumentType.getInteger(ctx, "distance"));
                          sendDistanceStatus(ctx, kind);
                          return 1;
                        }));
            root.then(kindNode);
          }

          dispatcher.register(root);
        });
  }

  private static void setClientFlag(String type, boolean on) {
    if (type.equals("tnt")) ClientSyncConfig.tntClientSync = on;
    else ClientSyncConfig.itemClientSync = on;
  }

  private static void setServerFlag(String type, boolean on) {
    if (type.equals("tnt")) ClientSyncConfig.tntServerSync = on;
    else ClientSyncConfig.itemServerSync = on;
  }

  /** 距离镜像的本地乐观更新，并请求服务端落地（服务端会广播回最新值）。 */
  private static void applyDistance(String kind, int value) {
    if (kind.equals("view")) ClientSyncConfig.viewDistanceOverride = value;
    else ClientSyncConfig.simulationDistanceOverride = value;
    ClientPlayNetworking.send(new SyncDistanceC2SPayload(kind, value));
  }

  private static String distanceName(String kind) {
    return kind.equals("view") ? "渲染距离" : "模拟距离";
  }

  private static String distanceStr(int value) {
    return value < 0 ? "默认" : value + " 区块";
  }

  private static void sendDistanceStatus(
      CommandContext<FabricClientCommandSource> ctx, String kind) {
    int v =
        kind.equals("view")
            ? ClientSyncConfig.viewDistanceOverride
            : ClientSyncConfig.simulationDistanceOverride;
    ctx.getSource().sendFeedback(Component.literal(distanceName(kind) + "： " + distanceStr(v)));
  }

  private static String nameOf(String type) {
    return type.equals("tnt") ? "TNT" : "物品";
  }

  private static String flagStr(boolean b) {
    return b ? "开启" : "关闭";
  }

  /** tick sprint 暂停期的统一后缀提示，让用户知道同步为何没生效。 */
  private static String pauseSuffix() {
    return ClientSyncConfig.serverPaused ? "（tick sprint 中，同步已自动暂停）" : "";
  }

  private static String visualStr() {
    return ClientSyncConfig.tntNoFlashScale ? "已移除" : "已恢复";
  }

  private static void sendVisualStatus(CommandContext<FabricClientCommandSource> ctx) {
    ctx.getSource().sendFeedback(Component.literal("TNT闪烁：" + visualStr()));
  }

  private static String timerStr() {
    return ClientSyncConfig.tntTickTimer ? "开启" : "关闭";
  }

  private static void sendTimerStatus(CommandContext<FabricClientCommandSource> ctx) {
    ctx.getSource().sendFeedback(Component.literal("TNT刻数标签：" + timerStr()));
  }

  private static void sendOneStatus(CommandContext<FabricClientCommandSource> ctx, String type) {
    boolean cs =
        type.equals("tnt") ? ClientSyncConfig.tntClientSync : ClientSyncConfig.itemClientSync;
    boolean ss =
        type.equals("tnt") ? ClientSyncConfig.tntServerSync : ClientSyncConfig.itemServerSync;
    ctx.getSource()
        .sendFeedback(
            Component.literal(
                "[LazyTNTutils] "
                    + nameOf(type)
                    + " → 本地修正："
                    + flagStr(cs)
                    + "，服务端同步："
                    + flagStr(ss)
                    + (type.equals("tnt") ? "，闪烁：" + visualStr() + "，刻数标签：" + timerStr() : "")
                    + pauseSuffix()));
  }

  private static void sendAllStatus(CommandContext<FabricClientCommandSource> ctx) {
    ctx.getSource()
        .sendFeedback(
            Component.literal(
                "[LazyTNTutils] TNT → 本地修正："
                    + flagStr(ClientSyncConfig.tntClientSync)
                    + "，服务端同步："
                    + flagStr(ClientSyncConfig.tntServerSync)
                    + "；物品 → 本地修正："
                    + flagStr(ClientSyncConfig.itemClientSync)
                    + "，服务端同步："
                    + flagStr(ClientSyncConfig.itemServerSync)
                    + "；TNT闪烁："
                    + visualStr()
                    + "，刻数标签："
                    + timerStr()
                    + "；渲染距离："
                    + distanceStr(ClientSyncConfig.viewDistanceOverride)
                    + "，模拟距离："
                    + distanceStr(ClientSyncConfig.simulationDistanceOverride)
                    + pauseSuffix()));
  }
}
