package com.lazytntutils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.entity.projectile.Projectile;

public final class Projectiles {

    private Projectiles() {}

    public static boolean isProjectile(Entity entity) {
        return entity instanceof Projectile
                || entity instanceof EyeOfEnder
                || entity instanceof EvokerFangs;
    }
}
