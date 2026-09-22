package com.lazytntutils.client;

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
import com.lazytntutils.SyncConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端入口：接收服务端下发的 TNT/物品 权威状态，缓存到对应 Storage； 注册客户端统一命令（同时管理“本地修正”与“服务端下发”两类开关），
 * 并接收服务端下发的开关镜像以在查询时显示。断连时清空实体状态并复位服务端开关镜像。
 */
public class LazyTNTUtilsClient implements ClientModInitializer {

  public static final Logger LOGGER = LoggerFactory.getLogger("lazytntutils");

  /** 命令里可切换的实体类别：TNT / 物品 / 弹射物 / 下落方块 / 经验球。 */
  private static final String[] TYPES = {"tnt", "item", "projectile", "fallingblock", "xp"};

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

    registerClientCommand();
  }

  /**
   * 注册客户端统一命令，同时管理两类开关： /lazytntutils # 查询全部（client + server） /lazytntutils <tnt|item> # 查询某类型的
   * client + server /lazytntutils <tnt|item> client [true|false] # 设置“本地修正”：开 =
   * 客户端不再本地模拟、按服务端权威修正显示（持久化）；关 = 退回本地模拟 /lazytntutils <tnt|item> server [true|false] #
   * 设置“服务端是否下发”（发包给服务端，由服务端落地；多人下决定有无权威数据） /lazytntutils tnt <visual|timer> [true|false] # TNT 视效 /
   * 刻数标签（纯客户端渲染，持久化） /lazytntutils tnt arrow [arrow|line|off] # TNT 动量矢量画法，三选项（见 MotionArrowMode）
   * client 开关在本地即时生效并写入客户端配置；server 开关经 SyncConfigC2SPayload 交给服务端， 服务端改完会广播新的镜像回来，因此所有提示与状态始终一致。
   * 回显粒度：不带子命令的查询（根 / 类型）分行给出状态——每个类别一行同步开关，纯渲染项再缩进一行；
   * 带 client / server 子命令时只回显该项自身。 与 visual / timer / arrow
   * 一致——它们也各自只报自己那一项，避免改一项却刷出一整行无关开关。 动量矢量（arrow）只对有该选项的类别（tnt / projectile）注册。
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

          for (String type : TYPES) {
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
              // client / server 子命令只回显自己那一项（不整行重播），避免改一侧却把另一侧也刷出来。
              sideNode.executes(
                  ctx -> {
                    sendSideStatus(ctx, type, side);
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
                            sendSideStatus(ctx, type, side);
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

            // 动量矢量画法：三选项（线段+箭头 / 仅线段 / 关闭），立即写入客户端配置。
            // 只对有该渲染选项的类别注册（tnt / projectile）。
            if (hasArrow(type)) {
              LiteralArgumentBuilder<FabricClientCommandSource> arrowNode =
                  LiteralArgumentBuilder.literal("arrow");
              arrowNode.executes(
                  ctx -> {
                    sendArrowStatus(ctx, type);
                    return 1;
                  });
              for (MotionArrowMode mode : MotionArrowMode.values()) {
                arrowNode.then(
                    LiteralArgumentBuilder.<FabricClientCommandSource>literal(mode.id())
                        .executes(
                            ctx -> {
                              setArrowMode(type, mode);
                              ClientSyncConfig.save();
                              sendArrowStatus(ctx, type);
                              return 1;
                            }));
              }
              typeNode.then(arrowNode);
            }
            root.then(typeNode);
          }

          // 原版蓝色朝向箭头（F3+B 每实体一根）的开关，见 ClientSyncConfig.showFacingArrow。
          LiteralArgumentBuilder<FabricClientCommandSource> facingNode =
              LiteralArgumentBuilder.literal("facing");
          facingNode.executes(
              ctx -> {
                sendFacingStatus(ctx);
                return 1;
              });
          facingNode.then(
              RequiredArgumentBuilder.<FabricClientCommandSource, Boolean>argument(
                      "enabled", BoolArgumentType.bool())
                  .executes(
                      ctx -> {
                        ClientSyncConfig.showFacingArrow = BoolArgumentType.getBool(ctx, "enabled");
                        ClientSyncConfig.save();
                        sendFacingStatus(ctx);
                        return 1;
                      }));
          root.then(facingNode);

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
    switch (type) {
      case "tnt" -> ClientSyncConfig.tntClientSync = on;
      case "item" -> ClientSyncConfig.itemClientSync = on;
      case "projectile" -> ClientSyncConfig.projectileClientSync = on;
      case "fallingblock" -> ClientSyncConfig.fallingBlockClientSync = on;
      case "xp" -> ClientSyncConfig.experienceOrbClientSync = on;
      default -> throw new IllegalArgumentException("未知类别：" + type);
    }
  }

  private static void setServerFlag(String type, boolean on) {
    switch (type) {
      case "tnt" -> ClientSyncConfig.tntServerSync = on;
      case "item" -> ClientSyncConfig.itemServerSync = on;
      case "projectile" -> ClientSyncConfig.projectileServerSync = on;
      case "fallingblock" -> ClientSyncConfig.fallingBlockServerSync = on;
      case "xp" -> ClientSyncConfig.experienceOrbServerSync = on;
      default -> throw new IllegalArgumentException("未知类别：" + type);
    }
  }

  /** 哪些类别有动量矢量渲染选项（目前 TNT 与弹射物）。 */
  private static boolean hasArrow(String type) {
    return type.equals("tnt") || type.equals("projectile");
  }

  private static void setArrowMode(String type, MotionArrowMode mode) {
    if (type.equals("tnt")) ClientSyncConfig.tntMotionArrow = mode;
    else ClientSyncConfig.projectileMotionArrow = mode;
  }

  private static MotionArrowMode arrowMode(String type) {
    return type.equals("tnt")
        ? ClientSyncConfig.tntMotionArrow
        : ClientSyncConfig.projectileMotionArrow;
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
    return switch (type) {
      case "tnt" -> "TNT";
      case "item" -> "物品";
      case "projectile" -> "弹射物";
      case "fallingblock" -> "下落方块";
      case "xp" -> "经验球";
      default -> type;
    };
  }

  private static String flagStr(boolean b) {
    return b ? "开启" : "关闭";
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

  private static void sendArrowStatus(CommandContext<FabricClientCommandSource> ctx, String type) {
    ctx.getSource()
        .sendFeedback(Component.literal(nameOf(type) + "动量矢量：" + arrowMode(type).label()));
  }

  private static void sendTimerStatus(CommandContext<FabricClientCommandSource> ctx) {
    ctx.getSource().sendFeedback(Component.literal("TNT刻数标签：" + timerStr()));
  }

  private static String facingStr() {
    return ClientSyncConfig.showFacingArrow ? "显示" : "隐藏";
  }

  private static void sendFacingStatus(CommandContext<FabricClientCommandSource> ctx) {
    ctx.getSource().sendFeedback(Component.literal("朝向箭头：" + facingStr()));
  }

  /** 回显一行文本（聊天栏一行一条，天然分行，不用自己拼换行）。 */
  private static void send(CommandContext<FabricClientCommandSource> ctx, String text) {
    ctx.getSource().sendFeedback(Component.literal(text));
  }

  /** 类别的同步开关行：本地修正 + 服务端同步。 */
  private static String typeLine(String type) {
    return nameOf(type)
        + " → 本地修正："
        + flagStr(clientFlag(type))
        + "，服务端同步："
        + flagStr(serverFlag(type));
  }

  /** 该类别除同步开关外的纯渲染项：单独成行、缩进两格跟在同步开关后面（没有则返回空串）。 */
  private static String extraStatus(String type) {
    if (type.equals("tnt")) {
      return "闪烁：" + visualStr() + "，刻数标签：" + timerStr() + "，动量矢量：" + arrowMode(type).label();
    }
    if (hasArrow(type)) {
      return "动量矢量：" + arrowMode(type).label();
    }
    return "";
  }

  private static void sendOneStatus(CommandContext<FabricClientCommandSource> ctx, String type) {
    send(ctx, typeLine(type));
    if (!extraStatus(type).isEmpty()) {
      send(ctx, "  " + extraStatus(type));
    }
  }

  /** 只回显某一侧（本地修正 / 服务端同步）的当前值，供 client / server 子命令使用。 */
  private static void sendSideStatus(
      CommandContext<FabricClientCommandSource> ctx, String type, String side) {
    boolean on = side.equals("client") ? clientFlag(type) : serverFlag(type);
    ctx.getSource()
        .sendFeedback(Component.literal(nameOf(type) + " → " + sideName(side) + "：" + flagStr(on)));
  }

  private static boolean clientFlag(String type) {
    return switch (type) {
      case "tnt" -> ClientSyncConfig.tntClientSync;
      case "item" -> ClientSyncConfig.itemClientSync;
      case "projectile" -> ClientSyncConfig.projectileClientSync;
      case "fallingblock" -> ClientSyncConfig.fallingBlockClientSync;
      case "xp" -> ClientSyncConfig.experienceOrbClientSync;
      default -> false;
    };
  }

  private static boolean serverFlag(String type) {
    return switch (type) {
      case "tnt" -> ClientSyncConfig.tntServerSync;
      case "item" -> ClientSyncConfig.itemServerSync;
      case "projectile" -> ClientSyncConfig.projectileServerSync;
      case "fallingblock" -> ClientSyncConfig.fallingBlockServerSync;
      case "xp" -> ClientSyncConfig.experienceOrbServerSync;
      default -> false;
    };
  }

  private static String sideName(String side) {
    return side.equals("client") ? "本地修正" : "服务端同步";
  }

  /**
   * 根命令的状态：分多行回显——标题一行，每个类别一行同步开关（有纯渲染项的类别再缩进补一行渲染项），
   * 最后两行报朝向箭头与距离。全挤在一行时聊天栏里扫不出重点，分行后每类各占一行。
   */
  private static void sendAllStatus(CommandContext<FabricClientCommandSource> ctx) {
    send(ctx, "[LazyTNTUtils]");
    for (String type : TYPES) {
      send(ctx, typeLine(type));
      if (!extraStatus(type).isEmpty()) {
        send(ctx, "  " + extraStatus(type));
      }
    }
    send(ctx, "朝向箭头：" + facingStr());
    send(
        ctx,
        "渲染距离："
            + distanceStr(ClientSyncConfig.viewDistanceOverride)
            + "，模拟距离："
            + distanceStr(ClientSyncConfig.simulationDistanceOverride));
  }
}
