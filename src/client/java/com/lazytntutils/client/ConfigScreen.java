package com.lazytntutils.client;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.SyncConfigC2SPayload;
import com.lazytntutils.network.SyncDistanceC2SPayload;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;

/**
 * 配置界面（YACL）：本 mod 唯一的开关入口（原 {@code /lazytntutils} 客户端命令已移除）。
 *
 * <p>## 文本一律走翻译键
 *
 * <p>本类**不出现任何字面文案**，所有标题 / 描述都是 {@code Component.translatable(...)}， 文案在 {@code
 * assets/lazytntutils/lang/<语言>.json} 里（现含 en_us 与 zh_cn，新增语言只需再放一个 json）。 键名统一以 {@code
 * lazytntutils.config.} 打头，约定如下：
 *
 * <ul>
 *   <li>标题 {@code .title}；分页 {@code .category.<页>}
 *   <li>实体类别分组 {@code .group.<kind>}（kind 即下面的 tnt / item / projectile / fallingblock / xp）
 *   <li>选项标题 {@code .option.<项>}，其描述 {@code .option.<项>.desc.1 .. .desc.N}
 * </ul>
 *
 * <p>两份语言文件的键必须**完全对齐**：某个键在其中一份里缺失，界面上就会直接显示原始键名（这是有意的—— 漏译因此一眼可见，不会静默退化）。增删描述行时记得同步改下面
 * {@code boolOption} 的 {@code descCount}。
 *
 * <p>## 绑定与生效时机
 *
 * <p>YACL 的 {@code binding} 是「默认值 / getter / setter」三元组，setter 只在用户确认（YACL 的 {@code
 * finishOrSave}）时才被调用， 而不是改一下控件就立刻写；随后 YACL 再回调 builder 的 {@code save(...)},那里统一落盘。 server 侧开关的 setter
 * 里同时发 {@link SyncConfigC2SPayload} 给服务端落地， 服务端随后广播新的镜像回来覆盖这里的显示值——所以界面上看到的一定是服务端真实值。
 *
 * <p>## 依赖形态
 *
 * <p>YACL 是**硬依赖**（没有它画不出界面）；ModMenu 只是挂按钮的入口（见 ModMenuIntegration）， 缺失时本类根本不会被加载，故为可选。
 */
public final class ConfigScreen {

  private ConfigScreen() {}

  /** 翻译键前缀，与 {@code assets/lazytntutils/lang/*.json} 里的键一一对应。 */
  private static final String KEY = "lazytntutils.config.";

  /**
   * 一个受支持的实体类别名。
   *
   * <p>值与配置文件、网络包里的标识一致，同时也是分组标题翻译键的后缀（{@code lazytntutils.config.group.<kind>}）， 三者刻意同名、不可随意改写。
   *
   * <p>与 {@code TrackedEntities} 的判定一一对应，但那里按类判断、这里要的是"类别名"，两侧无法共用。 单独包一层是为了让
   * {@code List<TrackedKind>} 比 {@code List<String>} 更能说明"这是受支持的类别"而不是任意字符串。
   */
  private record TrackedKind(String kind) {}

  private static final List<TrackedKind> KINDS =
      List.of(
          new TrackedKind("tnt"),
          new TrackedKind("item"),
          new TrackedKind("projectile"),
          new TrackedKind("fallingblock"),
          new TrackedKind("xp"));

  /** 由 ModMenu 调用；{@code parent} 是返回时要回到的界面。 */
  public static Screen create(Screen parent) {
    return YetAnotherConfigLib.createBuilder()
        .title(Component.translatable(KEY + "title"))
        .category(syncCategory())
        .category(renderCategory())
        .category(distanceCategory())
        // 保存回调在全部 setter 跑完之后执行，此时 ClientSyncConfig 里的值已是最终值。
        .save(ClientSyncConfig::save)
        .build()
        .generateScreen(parent);
  }

  /** 「同步」页：每类实体一组，含本地修正（client）与服务端同步（server）两个开关。 */
  private static ConfigCategory syncCategory() {
    ConfigCategory.Builder builder =
        ConfigCategory.createBuilder().name(Component.translatable(KEY + "category.sync"));

    for (TrackedKind kind : KINDS) {
      builder.group(
          OptionGroup.createBuilder()
              .name(Component.translatable(KEY + "group." + kind.kind()))
              .option(clientSyncOption(kind))
              .option(serverSyncOption(kind))
              .build());
    }
    return builder.build();
  }

  /** 「渲染」页：纯客户端的视觉项，与同步无关。 */
  private static ConfigCategory renderCategory() {
    return ConfigCategory.createBuilder()
        .name(Component.translatable(KEY + "category.render"))
        .option(
            boolOption(
                KEY + "option.tickTimer",
                1,
                true,
                () -> ClientSyncConfig.tntTickTimer,
                v -> ClientSyncConfig.tntTickTimer = v))
        .option(
            boolOption(
                KEY + "option.noFlashScale",
                1,
                true,
                () -> ClientSyncConfig.tntNoFlashScale,
                v -> ClientSyncConfig.tntNoFlashScale = v))
        .option(
            arrowOption(
                KEY + "option.tntArrow",
                () -> ClientSyncConfig.tntMotionArrow,
                v -> ClientSyncConfig.tntMotionArrow = v))
        .option(
            arrowOption(
                KEY + "option.projectileArrow",
                () -> ClientSyncConfig.projectileMotionArrow,
                v -> ClientSyncConfig.projectileMotionArrow = v))
        .option(
            boolOption(
                KEY + "option.explosionParticles",
                1,
                true,
                () -> ClientSyncConfig.simplifyExplosionParticles,
                v -> ClientSyncConfig.simplifyExplosionParticles = v))
        .option(
            boolOption(
                KEY + "option.facingArrow",
                1,
                false,
                () -> ClientSyncConfig.showFacingArrow,
                v -> ClientSyncConfig.showFacingArrow = v))
        .build();
  }

  /** 「距离」页：视距与模拟距离的运行期覆盖值，滑块最左端 = 退回原版设置。 */
  private static ConfigCategory distanceCategory() {
    return ConfigCategory.createBuilder()
        .name(Component.translatable(KEY + "category.distance"))
        .option(distanceOption("view", KEY + "option.viewDistance"))
        .option(distanceOption("sim", KEY + "option.simDistance"))
        .build();
  }

  private static Option<Boolean> clientSyncOption(TrackedKind kind) {
    return boolOption(
        KEY + "option.clientSync",
        1,
        // 默认值取 ClientSyncConfig 的出厂默认（TNT 为 true，其余 false），与字段初始化同源。
        ClientSyncConfig.defaultClientSync(kind.kind()),
        () -> clientFlag(kind.kind()),
        v -> setClientFlag(kind.kind(), v));
  }

  private static Option<Boolean> serverSyncOption(TrackedKind kind) {
    return boolOption(
        KEY + "option.serverSync",
        1,
        false,
        () -> serverFlag(kind.kind()),
        v -> setServerFlag(kind.kind(), v));
  }

  /**
   * 布尔开关。
   *
   * @param key 选项标题的翻译键；描述取 {@code <key>.desc.1 .. .desc.<descCount>}
   *            —— 所有选项在语言文件里都按这个编号约定命名，加一行描述只需改数字与 json。
   */
  private static Option<Boolean> boolOption(
      String key, int descCount, boolean def, Supplier<Boolean> get, Consumer<Boolean> set) {
    return Option.<Boolean>createBuilder()
        .name(Component.translatable(key))
        .description(description(key, descCount))
        .binding(def, get, set)
        .controller(BooleanControllerBuilder::create)
        .build();
  }

  /** 动量箭头画法：三选一的下拉，显示名取 MotionArrowMode 的翻译键（两个选项共用同一份描述）。 */
  private static Option<MotionArrowMode> arrowOption(
      String key, Supplier<MotionArrowMode> get, Consumer<MotionArrowMode> set) {
    return Option.<MotionArrowMode>createBuilder()
        .name(Component.translatable(key))
        .description(description(KEY + "option.arrow", 1))
        .binding(MotionArrowMode.ARROW, get, set)
        .controller(
            opt ->
                EnumControllerBuilder.create(opt)
                    .enumClass(MotionArrowMode.class)
                    .formatValue(mode -> Component.translatable(mode.translationKey())))
        .build();
  }

  /**
   * 距离：滑块，最左端 -1 =「默认（不覆盖）」，向右到 MAX_DISTANCE。
   *
   * <p>刻意**不持久化**：距离是"这次调试临时压低模拟距离"这类一次性操作，落盘后就会和 server.properties / 视频设置形成两个"谁优先"的来源，
   * 下次进来究竟以哪个为准会让人困惑。所以这里只改运行期内存值—— 换存档、退出到主界面、重启服务端都会自动回到「默认」，无需手动还原。
   */
  private static Option<Integer> distanceOption(String kind, String key) {
    return Option.<Integer>createBuilder()
        .name(Component.translatable(key))
        .description(description(KEY + "option.distance", 1))
        .binding(-1, () -> distance(kind), v -> setDistance(kind, v))
        .controller(
            opt ->
                IntegerSliderControllerBuilder.create(opt)
                    .range(-1, SyncConfig.MAX_DISTANCE)
                    .step(1)
                    .formatValue(ConfigScreen::distanceText))
        .build();
  }

  /** 滑块的当前值文案：最左端（负数）显示「默认」，其余显示区块数。 */
  private static Component distanceText(Integer value) {
    return value < 0
        ? Component.translatable(KEY + "distance.default")
        : Component.translatable(KEY + "distance.chunks", value);
  }

  /** 按约定拼出 {@code <key>.desc.1 .. .desc.<count>} 的多行描述。 */
  private static OptionDescription description(String key, int count) {
    Component[] lines = new Component[count];
    for (int i = 1; i <= count; i++) {
      lines[i - 1] = Component.translatable(key + ".desc." + i);
    }
    return OptionDescription.of(lines);
  }

  // ---- 类别开关的读写 ----

  private static boolean clientFlag(String kind) {
    return switch (kind) {
      case "tnt" -> ClientSyncConfig.tntClientSync;
      case "item" -> ClientSyncConfig.itemClientSync;
      case "projectile" -> ClientSyncConfig.projectileClientSync;
      case "fallingblock" -> ClientSyncConfig.fallingBlockClientSync;
      case "xp" -> ClientSyncConfig.experienceOrbClientSync;
      default -> false;
    };
  }

  private static void setClientFlag(String kind, boolean on) {
    switch (kind) {
      case "tnt" -> ClientSyncConfig.tntClientSync = on;
      case "item" -> ClientSyncConfig.itemClientSync = on;
      case "projectile" -> ClientSyncConfig.projectileClientSync = on;
      case "fallingblock" -> ClientSyncConfig.fallingBlockClientSync = on;
      case "xp" -> ClientSyncConfig.experienceOrbClientSync = on;
      default -> throw new IllegalArgumentException("未知类别：" + kind);
    }
  }

  private static boolean serverFlag(String kind) {
    return switch (kind) {
      case "tnt" -> ClientSyncConfig.tntServerSync;
      case "item" -> ClientSyncConfig.itemServerSync;
      case "projectile" -> ClientSyncConfig.projectileServerSync;
      case "fallingblock" -> ClientSyncConfig.fallingBlockServerSync;
      case "xp" -> ClientSyncConfig.experienceOrbServerSync;
      default -> false;
    };
  }

  /**
   * 改 server 侧开关：先本地乐观更新镜像（界面立刻反映用户操作），再请求服务端落地。
   *
   * <p>服务端改完会广播新的镜像回来覆盖这里的值，因此最终显示的一定是服务端的真实状态。
   * 未连接 / 服务端没装本 mod 时 {@code canSend} 为 false，跳过发包（此时改的只是本地镜像， 无所谓——它本来就只用于显示与"是否套用下发数据"的判断）。
   */
  private static void setServerFlag(String kind, boolean on) {
    switch (kind) {
      case "tnt" -> ClientSyncConfig.tntServerSync = on;
      case "item" -> ClientSyncConfig.itemServerSync = on;
      case "projectile" -> ClientSyncConfig.projectileServerSync = on;
      case "fallingblock" -> ClientSyncConfig.fallingBlockServerSync = on;
      case "xp" -> ClientSyncConfig.experienceOrbServerSync = on;
      default -> throw new IllegalArgumentException("未知类别：" + kind);
    }
    if (ClientPlayNetworking.canSend(SyncConfigC2SPayload.ID)) {
      ClientPlayNetworking.send(new SyncConfigC2SPayload(kind, on));
    }
  }

  private static int distance(String kind) {
    return kind.equals("view")
        ? ClientSyncConfig.viewDistanceOverride
        : ClientSyncConfig.simulationDistanceOverride;
  }

  /** 与 {@link #setServerFlag} 同理：本地镜像先更新，再请求服务端落地（值先经 clamp 保证合法）。 */
  private static void setDistance(String kind, int value) {
    int clamped = SyncConfig.clampDistance(value);
    if (kind.equals("view")) ClientSyncConfig.viewDistanceOverride = clamped;
    else ClientSyncConfig.simulationDistanceOverride = clamped;
    if (ClientPlayNetworking.canSend(SyncDistanceC2SPayload.ID)) {
      ClientPlayNetworking.send(new SyncDistanceC2SPayload(kind, clamped));
    }
  }
}
