package com.lazytntutils.server;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.entity.EntityTypeTest;
import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.ExperienceOrbUpdatePayload;

/**
 * 服务端经验球追踪器：逐 tick 把玩家附近的经验球权威 pos/vel 发给该玩家， 避免弱加载区的经验球在客户端自行漂移 / 靠向玩家。
 *
 * <p>骨架（取列表 / 裁剪 / 下发）在 {@link EntitySyncTracker}，这里只说明"同步什么、怎么打包"。 只发位置与速度两项：合并、吸取、消失全部由服务端权威驱动。
 *
 * <p>经验球数量可能很大（一次大爆炸几百个），因此本类别默认关闭，只在确实要观察时开。
 */
public final class ServerExperienceOrbTracker extends EntitySyncTracker<ExperienceOrb> {

  private static final ServerExperienceOrbTracker INSTANCE = new ServerExperienceOrbTracker();

  private ServerExperienceOrbTracker() {}

  public static void register() {
    ServerTickEvents.END_SERVER_TICK.register(INSTANCE::onTick);
  }

  @Override
  protected boolean isEnabled() {
    return SyncConfig.experienceOrbServerSync;
  }

  @Override
  protected EntityTypeTest<Entity, ExperienceOrb> typeTest() {
    return EntityTypeTest.forClass(ExperienceOrb.class);
  }

  @Override
  protected CustomPacketPayload buildPayload(List<ExperienceOrb> entities) {
    List<ExperienceOrbUpdatePayload.Entry> entries = new ArrayList<>(entities.size());
    for (ExperienceOrb orb : entities) {
      entries.add(
          new ExperienceOrbUpdatePayload.Entry(
              orb.getUUID(),
              orb.getX(),
              orb.getY(),
              orb.getZ(),
              orb.getDeltaMovement().x,
              orb.getDeltaMovement().y,
              orb.getDeltaMovement().z));
    }
    return new ExperienceOrbUpdatePayload(entries);
  }
}
