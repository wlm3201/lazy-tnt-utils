package net.orbitalstrike.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * 实体权威状态的逐 tick 下发（模板方法模式）：把玩家附近指定类型实体的权威状态发给该玩家， 供客户端修正本地表现——弱加载区块里服务端不 tick、客户端却在本地模拟，两者必然错位。
 *
 * <p>TNT 与物品共用这套流程，只有「是否开启」「实体类型」「如何打包」三点不同， 由子类给出；取列表、距离裁剪、下发这套骨架在这里统一实现。
 *
 * <p>## 为什么不用 AABB 空间查询
 *
 * <p>距离裁剪要的是"决定发给谁"，跟"怎么把实体找出来"是两件事。 用 getEntities(AABB) 会把前者耦合到 chunk 空间索引上：160 格半径要遍历 20×20 = 400
 * 个 chunk 的实体桶，而本 mod 关心的实体通常只有几颗，等于为几颗 TNT 扫了上百个桶。
 *
 * <p>这里改成走实体类型索引拿到全部候选，再在内存里判距离： - CPU：遍历一次实体列表（且按维度缓存，同维度的多个玩家共享），而不是上万次桶访问； - 带宽：判距离这一步与 AABB
 * 方案完全等价，发出去的包一模一样。
 *
 * <p>## 与原版同步的分工
 *
 * <p>原版是变化驱动的增量同步（Observer + Dirty Flag）：实体不 tick 就没有变化，也就一个包都不发，
 * 于是客户端继续自己模拟、越跑越偏。本类是反向思路——周期性的全量快照复制 （Snapshot Replication），客户端不做预测，因此"静止"这件事也能被准确传达。
 */
public abstract class EntitySyncTracker<E extends Entity> {

  /** 只同步玩家此半径（方块）内的实体，纯粹为了限制带宽，与区块加载层级无关。 160 格明显大于常见视距（默认 10 区块 = 160 格起），足以覆盖玩家实际能看到的全部弱加载区。 */
  private static final double SYNC_RADIUS = 160.0;

  private static final double SYNC_RADIUS_SQ = SYNC_RADIUS * SYNC_RADIUS;

  /** 该类型的同步是否开启（对应 SyncConfig 里的 server 侧开关）。关闭时整体跳过。 */
  protected abstract boolean isEnabled();

  /** 要同步的实体类型。用类型索引直接取，避免 AABB 空间查询。 */
  protected abstract EntityTypeTest<Entity, E> typeTest();

  /** 把这一批（已按距离裁剪过的）实体打包成下发的 payload。 */
  protected abstract CustomPacketPayload buildPayload(List<E> entities);

  /** 每个服务端 tick 末尾调用：按玩家下发各自附近的实体状态。 */
  public void onTick(MinecraftServer server) {
    if (!isEnabled()) return;
    if (TickSprintGuard.isSprinting()) return; // /tick sprint 期间整体停发，见 TickSprintGuard

    // 同一维度的实体列表每 tick 只取一次，多个玩家共享，避免 N 个玩家取 N 遍。
    Map<ServerLevel, List<E>> entitiesByLevel = new HashMap<>();

    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      List<E> all = entitiesByLevel.computeIfAbsent((ServerLevel) player.level(), this::queryAll);

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

  /** 取该维度内全部目标类型实体（不过滤距离，距离在调用处按玩家裁剪）。 */
  private List<E> queryAll(ServerLevel level) {
    List<E> result = new ArrayList<>();
    level.getEntities(typeTest(), entity -> true, result);
    return result;
  }
}
