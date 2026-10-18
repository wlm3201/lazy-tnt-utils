package com.lazytntutils.server;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.ItemUpdatePayload;

/**
 * 服务端物品实体追踪器：逐 tick 把玩家附近的物品实体权威 pos/vel 发给该玩家， 避免弱加载物品在客户端自行下落/漂移。
 *
 * <p>骨架（取列表 / 裁剪 / 下发）在 {@link EntitySyncTracker}，这里只说明"同步什么、怎么打包"。
 * 只同步位置与速度两项：物品的消失(age)与拾取(pickupDelay)均由服务端权威、通过移除实体包驱动， 客户端不模拟就不会出错，无需同步。
 */
public final class ServerItemTracker extends EntitySyncTracker<ItemEntity> {

  private static final ServerItemTracker INSTANCE = new ServerItemTracker();

  private ServerItemTracker() {}

  public static void register() {
    ServerTickEvents.END_SERVER_TICK.register(INSTANCE::onTick);
  }

  @Override
  protected boolean isEnabled() {
    return SyncConfig.itemServerSync;
  }

  @Override
  protected EntityTypeTest<Entity, ItemEntity> typeTest() {
    return EntityTypeTest.forClass(ItemEntity.class);
  }

  @Override
  protected CustomPacketPayload buildPayload(List<ItemEntity> entities) {
    List<ItemUpdatePayload.ItemEntry> entries = new ArrayList<>(entities.size());
    for (ItemEntity item : entities) {
      entries.add(
          new ItemUpdatePayload.ItemEntry(
              item.getUUID(),
              item.getX(),
              item.getY(),
              item.getZ(),
              item.getDeltaMovement().x,
              item.getDeltaMovement().y,
              item.getDeltaMovement().z));
    }
    return new ItemUpdatePayload(entries);
  }
}
