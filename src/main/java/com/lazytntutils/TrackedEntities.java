package com.lazytntutils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;

/**
 * "哪些实体属于本 mod 关注的类别"的唯一判定点（与开关无关）。
 *
 * <p>用途有两处：同步 / 冻结按类别分别开关（见 ClientEntityFreezeMixin）， 而纯渲染项（隐藏无意义的蓝色朝向箭头）需要对**全部**类别一视同仁。
 *
 * <p>判据是"客户端会本地跑 tick、且在弱加载区会因此与服务端脱节"的实体： TNT（引信 + 运动）、物品、弹射物（见 {@link Projectiles}）、下落方块、经验球。 矿车
 * / 船与生物刻意不在内——前者可被玩家乘坐，客户端本地演算是原版刻意的设计；后者由服务端或玩家驱动。
 */
public final class TrackedEntities {

  private TrackedEntities() {}

  public static boolean isTracked(Entity entity) {
    return entity instanceof PrimedTnt
        || entity instanceof ItemEntity
        || Projectiles.isProjectile(entity)
        || entity instanceof FallingBlockEntity
        || entity instanceof ExperienceOrb;
  }
}
