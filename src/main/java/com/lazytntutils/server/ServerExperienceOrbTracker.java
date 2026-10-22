package com.lazytntutils.server;

import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.ExperienceOrbUpdatePayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.entity.EntityTypeTest;

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
