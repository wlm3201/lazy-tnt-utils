package com.lazytntutils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;

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
