package net.orbitalstrike;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * 服务端运行期配置：是否采集并下发 TNT/物品的权威状态（server 侧）。
 *
 * <p>- tntServerSync / itemServerSync：服务端是否采集并下发对应实体（默认关）。
 *
 * <p>服务端两个下发默认关闭（逐 tick 发包占带宽，需要时才开）；默认仅 TNT 的“本地修正” （tntClientSync，见 ClientSyncConfig）开启。配置持久化到
 * config/lazytntutils.properties （启动时 load，改动时 save），重启后保留上次的设置。可通过客户端统一命令 /lazytntutils sync
 * &lt;tnt|item&gt; server ... 切换（经 SyncConfigC2SPayload 交由服务端落地）。
 *
 * <p>客户端是否开启本地修正（不再本地模拟、按权威状态呈现），由客户端自身配置 ClientSyncConfig 控制（见
 * LazyTNTutilsClient），两者独立：服务端控制“发不发”， 客户端控制“修不修正”（关则退回本地模拟）。
 */
public final class SyncConfig {

  private SyncConfig() {}

  public static boolean tntServerSync = false;
  public static boolean itemServerSync = false;

  /**
   * 运行时覆盖服务端视距 / 模拟距离（区块数）。-1 = 不覆盖（保持原版 / server.properties 的值）。
   *
   * <p>与原版 / carpet 不同：0 与 1 都是**真实值**而非"回退到默认"—— simulationDistance = 0 → 只有玩家所在区块 entity tick；1 →
   * 玩家中心 3×3 entity tick。
   *
   * <p>刻意**不做持久化**：一旦写进 config/lazytntutils.properties，就会和 server.properties
   * 形成两个"谁优先"的来源，重启后究竟以哪个为准会让人困惑。因此这里只作为运行期覆盖，需要时用 /lazytntutils view|sim 临时改。
   *
   * <p>生命周期：这两个值是纯静态状态、不落盘，只保证在**一次服务端进程内**有效。专用服上"一次进程"= 一次开服，重启即回到 server.properties；
   * 单人下集成服务端与游戏同进程，静态值会跨存档存活（表现为"换存档仍保留、退游戏不保留"）， 故关服时由 {@link #resetDistance()} 显式清掉，使单人下"退出存档 =
   * 回到视频设置"。
   */
  public static int viewDistanceOverride = -1;

  public static int simulationDistanceOverride = -1;

  /** 允许的取值上限（原版区块层级上限为 32）。 */
  public static final int MAX_DISTANCE = 32;

  private static Path configFile() {
    return FabricLoader.getInstance().getConfigDir().resolve("lazytntutils.properties");
  }

  /** 启动时调用：若配置文件存在则读取并覆盖默认值。读取失败则保留默认。 */
  public static void load() {
    Path file = configFile();
    if (!Files.exists(file)) return;
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(file)) {
      props.load(in);
    } catch (IOException e) {
      LazyTNTutils.LOGGER.warn("[LazyTNTutils] 读取服务端配置文件失败，使用默认配置", e);
      return;
    }
    tntServerSync = parse(props, "tntServerSync", tntServerSync);
    itemServerSync = parse(props, "itemServerSync", itemServerSync);
    // 视距 / 模拟距离刻意不读取：见字段注释，不做持久化。
  }

  private static boolean parse(Properties props, String key, boolean fallback) {
    String v = props.getProperty(key);
    return v == null ? fallback : Boolean.parseBoolean(v.trim());
  }

  /** 把值限制在 [-1, MAX_DISTANCE]；越界的值按"不覆盖"处理，避免下发非法距离。 */
  public static int clampDistance(int value) {
    if (value < -1) return -1;
    return Math.min(value, MAX_DISTANCE);
  }

  /** 关服（含单人退出存档）时丢弃运行期距离覆盖，回到 -1 = 不覆盖。见字段注释的生命周期说明。 */
  public static void resetDistance() {
    viewDistanceOverride = -1;
    simulationDistanceOverride = -1;
  }

  /** 配置变更后调用：把当前开关写回 config/lazytntutils.properties。 */
  public static void save() {
    Properties props = new Properties();
    props.setProperty("tntServerSync", Boolean.toString(tntServerSync));
    props.setProperty("itemServerSync", Boolean.toString(itemServerSync));
    // 视距 / 模拟距离刻意不写入：见字段注释，不做持久化。
    Path file = configFile();
    try {
      Files.createDirectories(file.getParent());
      try (OutputStream out = Files.newOutputStream(file)) {
        props.store(out, "LazyTNTutils server sync config");
      }
    } catch (IOException e) {
      LazyTNTutils.LOGGER.warn("[LazyTNTutils] 写入服务端配置文件失败", e);
    }
  }
}
