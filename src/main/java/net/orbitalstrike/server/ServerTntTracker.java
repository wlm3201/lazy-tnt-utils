package net.orbitalstrike.server;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.orbitalstrike.SyncConfig;
import net.orbitalstrike.network.TntUpdatePayload;

/**
 * 服务端 TNT 追踪器：逐 tick 把玩家附近的 TNT 权威 pos/vel/引信发给该玩家， 供客户端修正本地 TNT（防弱加载区客户端提前爆炸、以及多人下的实体分离偏差）。
 *
 * <p>骨架（取列表 / 裁剪 / 下发）在 {@link EntitySyncTracker}，这里只说明"同步什么、怎么打包"。 引信必须每次都发：弱加载区服务端不
 * tick，引信"没有减少"这件事无法用原版的 变化驱动同步表达（没变化 = 不发包），而这正是客户端提前爆炸的根因。
 */
public final class ServerTntTracker extends EntitySyncTracker<PrimedTnt> {

  private static final ServerTntTracker INSTANCE = new ServerTntTracker();

  private ServerTntTracker() {}

  public static void register() {
    ServerTickEvents.END_SERVER_TICK.register(INSTANCE::onTick);
  }

  @Override
  protected boolean isEnabled() {
    return SyncConfig.tntServerSync;
  }

  @Override
  protected EntityTypeTest<Entity, PrimedTnt> typeTest() {
    return EntityTypeTest.forClass(PrimedTnt.class);
  }

  @Override
  protected CustomPacketPayload buildPayload(List<PrimedTnt> entities) {
    List<TntUpdatePayload.TntEntry> entries = new ArrayList<>(entities.size());
    for (PrimedTnt tnt : entities) {
      entries.add(
          new TntUpdatePayload.TntEntry(
              tnt.getUUID(),
              tnt.getX(),
              tnt.getY(),
              tnt.getZ(),
              tnt.getDeltaMovement().x,
              tnt.getDeltaMovement().y,
              tnt.getDeltaMovement().z,
              tnt.getFuse()));
    }
    return new TntUpdatePayload(entries);
  }
}
