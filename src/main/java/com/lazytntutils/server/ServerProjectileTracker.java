package com.lazytntutils.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import com.lazytntutils.Projectiles;
import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.ProjectileUpdatePayload;

/**
 * 服务端弹射物追踪器：逐 tick 把玩家附近的弹射物权威 pos/vel 发给该玩家， 避免弱加载区的箭 / 火球 / 雪球等在客户端继续飞、甚至提前命中消失。
 *
 * <p>骨架（取列表 / 裁剪 / 下发）在 {@link EntitySyncTracker}，这里只说明"同步什么、怎么打包"。
 *
 * <p>类型维度上取 {@code Entity} 再用 {@link Projectiles} 过滤：弹射物不是一个类而是一组类 （Projectile 子类 + 末影之眼 +
 * 唤魔者尖牙），只能这样表达；底层本来就是全实体遍历，无额外开销。 只发位置与速度两项，命中 / 消失 / 拾取全部由服务端权威驱动。
 */
public final class ServerProjectileTracker extends EntitySyncTracker<Entity> {

  private static final ServerProjectileTracker INSTANCE = new ServerProjectileTracker();

  private ServerProjectileTracker() {}

  public static void register() {
    ServerTickEvents.END_SERVER_TICK.register(INSTANCE::onTick);
  }

  @Override
  protected boolean isEnabled() {
    return SyncConfig.projectileServerSync;
  }

  @Override
  protected EntityTypeTest<Entity, Entity> typeTest() {
    return EntityTypeTest.forClass(Entity.class);
  }

  @Override
  protected Predicate<Entity> filter() {
    return Projectiles::isProjectile;
  }

  @Override
  protected CustomPacketPayload buildPayload(List<Entity> entities) {
    List<ProjectileUpdatePayload.Entry> entries = new ArrayList<>(entities.size());
    for (Entity projectile : entities) {
      entries.add(
          new ProjectileUpdatePayload.Entry(
              projectile.getUUID(),
              projectile.getX(),
              projectile.getY(),
              projectile.getZ(),
              projectile.getDeltaMovement().x,
              projectile.getDeltaMovement().y,
              projectile.getDeltaMovement().z));
    }
    return new ProjectileUpdatePayload(entries);
  }
}
