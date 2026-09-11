package net.orbitalstrike.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.orbitalstrike.LazyTNTutilsClient;

/**
 * 客户端侧同步配置。
 *
 * <p>- tntClientSync / itemClientSync：是否开启“本地修正”（由客户端命令控制，持久化到
 * lazytntutils-client.properties，与服务端是否安装本 mod 无关）。开 = 本地不再模拟， 实体表现每 tick 直接按服务端权威修正（单人直读 /
 * 多人网络下发，取不到则冻结）； 关 = 保留本地模拟（多人有服务端下发时仅在 tick 末尾纠回）。 - tntServerSync / itemServerSync：服务端开关镜像（来自
 * SyncConfigS2CPayload，不持久化）， 多人下据此判断服务端是否仍在并发，也用于命令查询显示。
 *
 * <p>默认仅 tntClientSync 开启：TNT 本地修正是纯客户端本地行为，不发包、不依赖服务端是否 安装本 mod，最“无副作用”，适合开箱即用（单人 / 弱加载 TNT
 * 显示）。其余默认关闭—— itemClientSync 关（物品走原版模拟）；两个 server 镜像默认关，与服务端新默认 （SyncConfig）一致，收到服务端广播前视为“未下发”。
 */
public final class ClientSyncConfig {

  private ClientSyncConfig() {}

  /** 本地偏好：是否开启本地修正（持久化到 lazytntutils-client.properties）。默认仅 TNT 开启。 */
  public static boolean tntClientSync = true;

  public static boolean itemClientSync = false;

  /**
   * 是否去掉 TNT 的白色闪烁与爆炸前缩放（纯客户端渲染，持久化）。默认开启： 弱加载下这两个效果会同 partialTicks 一起抖动，既难看也遮挡观察。
   * 关掉则保留原版表现；无论开关如何，"冻结时抖动"都会被永久修正（见 TntRendererMixin）。
   */
  public static boolean tntNoFlashScale = true;

  /** 是否在每颗 TNT 头顶显示剩余**游戏刻**数（纯客户端渲染，持久化）。默认开启： 生电关心刻级时序，市面上同类模组只给秒数。见 TntTickOverlayRenderer。 */
  public static boolean tntTickTimer = true;

  /** 服务端开关镜像（来自 SyncConfigS2CPayload，不持久化）：多人下“网络权威”分支据此判断是否仍有下发，也用于命令查询显示。默认关，与服务端默认一致。 */
  public static boolean tntServerSync = false;

  public static boolean itemServerSync = false;

  /**
   * 服务端是否处于 tick sprint 暂停期（来自 SyncPauseS2CPayload，不持久化）： sprint 期间服务端停发，客户端必须同步停止套用缓存，否则会把实体纠回
   * sprint 前的陈旧位置。
   */
  public static boolean serverPaused = false;

  /**
   * 服务端距离覆盖值的镜像（来自 SyncConfigS2CPayload，不持久化）：-1 = 不覆盖。仅用于查询显示，
   * 真正的强制生效在服务端事件点完成（DistanceEnforcer）；单人下集成服务端每 tick 会按视频设置重设视距，由 IntegratedServerMixin
   * 把覆盖值喂给原版那次同步。这份镜像本地不落盘，断连时随 {@link #reset()} 一起回到 -1。
   */
  public static int viewDistanceOverride = -1;

  public static int simulationDistanceOverride = -1;

  private static Path configFile() {
    return FabricLoader.getInstance().getConfigDir().resolve("lazytntutils-client.properties");
  }

  /** 启动时调用：若配置文件存在则读取并覆盖默认值。 */
  public static void load() {
    Path file = configFile();
    if (!Files.exists(file)) return;
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(file)) {
      props.load(in);
    } catch (IOException e) {
      LazyTNTutilsClient.LOGGER.warn("[LazyTNTutils] 读取客户端配置文件失败，使用默认配置", e);
      return;
    }
    tntClientSync = parse(props, "tntClientSync", tntClientSync);
    itemClientSync = parse(props, "itemClientSync", itemClientSync);
    tntNoFlashScale = parse(props, "tntNoFlashScale", tntNoFlashScale);
    tntTickTimer = parse(props, "tntTickTimer", tntTickTimer);
  }

  private static boolean parse(Properties props, String key, boolean fallback) {
    String v = props.getProperty(key);
    return v == null ? fallback : Boolean.parseBoolean(v.trim());
  }

  /** 配置变更后调用：把当前开关写回 config/lazytntutils-client.properties。 */
  public static void save() {
    Properties props = new Properties();
    props.setProperty("tntClientSync", Boolean.toString(tntClientSync));
    props.setProperty("itemClientSync", Boolean.toString(itemClientSync));
    props.setProperty("tntNoFlashScale", Boolean.toString(tntNoFlashScale));
    props.setProperty("tntTickTimer", Boolean.toString(tntTickTimer));
    Path file = configFile();
    try {
      Files.createDirectories(file.getParent());
      try (OutputStream out = Files.newOutputStream(file)) {
        props.store(out, "LazyTNTutils client sync config");
      }
    } catch (IOException e) {
      LazyTNTutilsClient.LOGGER.warn("[LazyTNTutils] 写入客户端配置文件失败", e);
    }
  }

  /** 断开连接时把“服务端下发”镜像复位为关闭（不持久化），避免残留上一服配置导致误冻结； 本地修正偏好（tnt/itemClientSync）保留，因为它反映本机用户习惯。 */
  public static void reset() {
    tntServerSync = false;
    itemServerSync = false;
    serverPaused = false;
    viewDistanceOverride = -1;
    simulationDistanceOverride = -1;
  }
}
