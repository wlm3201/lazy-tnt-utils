package com.lazytntutils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * "哪些实体算投掷物"的唯一判定点（服务端追踪器与客户端冻结共用）。
 *
 * <p>26.x 里 {@link Projectile} 已经是**抽象类**（不是接口），绝大多数投掷物都继承它： 箭 / 光灵箭 / 三叉戟（AbstractArrow）、雪球 / 鸡蛋 /
 * 末影珍珠 / 喷溅与滞留药水 / 附魔之瓶（ThrowableProjectile）、 恶魂火球 / 烈焰人小火球 / 凋灵之首 / 龙息火球 / 风弹 /
 * 旋风弹（AbstractHurtingProjectile）、羊驼唾沫、潜影弹、烟花火箭、钓鱼钩。 一条 {@code instanceof Projectile} 即可全覆盖，不需要按
 * wiki 逐个类型登记。
 *
 * <p>例外是 wiki 弹射物条目里还有两个原版没让它继承 Projectile 的： 末影之眼（{@link EyeOfEnder}，extends Entity）与唤魔者尖牙（{@link
 * EvokerFangs}，extends Entity）。 两者同样在客户端本地跑 tick，故在这里一并纳入。
 */
public final class Projectiles {

  private Projectiles() {}

  public static boolean isProjectile(Entity entity) {
    return entity instanceof Projectile
        || entity instanceof EyeOfEnder
        || entity instanceof EvokerFangs;
  }
}
