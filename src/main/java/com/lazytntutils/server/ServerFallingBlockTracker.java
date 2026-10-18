package com.lazytntutils.server;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.FallingBlockUpdatePayload;

/**
 * 服务端下落方块追踪器：逐 tick 把玩家附近的下落方块权威 pos/vel 发给该玩家， 避免弱加载区的沙 / 砂砾 / 混凝土 / 铁砧在客户端一路自行下落穿地。
 *
 * <p>骨架（取列表 / 裁剪 / 下发）在 {@link EntitySyncTracker}，这里只说明"同步什么、怎么打包"。
 * 只发位置与速度两项：何时落地成块由服务端权威、通过实体移除包驱动， 客户端不模拟就不会出错。
 */
public final class ServerFallingBlockTracker extends EntitySyncTracker<FallingBlockEntity> {

  private static final ServerFallingBlockTracker INSTANCE = new ServerFallingBlockTracker();

  private ServerFallingBlockTracker() {}

  public static void register() {
    ServerTickEvents.END_SERVER_TICK.register(INSTANCE::onTick);
  }

  @Override
  protected boolean isEnabled() {
    return SyncConfig.fallingBlockServerSync;
  }

  @Override
  protected EntityTypeTest<Entity, FallingBlockEntity> typeTest() {
    return EntityTypeTest.forClass(FallingBlockEntity.class);
  }

  @Override
  protected CustomPacketPayload buildPayload(List<FallingBlockEntity> entities) {
    List<FallingBlockUpdatePayload.Entry> entries = new ArrayList<>(entities.size());
    for (FallingBlockEntity block : entities) {
      entries.add(
          new FallingBlockUpdatePayload.Entry(
              block.getUUID(),
              block.getX(),
              block.getY(),
              block.getZ(),
              block.getDeltaMovement().x,
              block.getDeltaMovement().y,
              block.getDeltaMovement().z));
    }
    return new FallingBlockUpdatePayload(entries);
  }
}
