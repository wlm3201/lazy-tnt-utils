package com.lazytntutils.server;

import com.lazytntutils.Projectiles;
import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.ProjectileUpdatePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;

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
