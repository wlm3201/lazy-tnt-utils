package com.lazytntutils.server;

import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.FallingBlockUpdatePayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.entity.EntityTypeTest;

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
