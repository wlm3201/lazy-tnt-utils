package com.lazytntutils.server;

import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.ItemUpdatePayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;

public final class ServerItemTracker extends EntitySyncTracker<ItemEntity> {

    private static final ServerItemTracker INSTANCE = new ServerItemTracker();

    private ServerItemTracker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(INSTANCE::onTick);
    }

    @Override
    protected boolean isEnabled() {
        return SyncConfig.itemServerSync;
    }

    @Override
    protected EntityTypeTest<Entity, ItemEntity> typeTest() {
        return EntityTypeTest.forClass(ItemEntity.class);
    }

    @Override
    protected CustomPacketPayload buildPayload(List<ItemEntity> entities) {
        List<ItemUpdatePayload.ItemEntry> entries = new ArrayList<>(entities.size());
        for (ItemEntity item : entities) {
            entries.add(
                    new ItemUpdatePayload.ItemEntry(
                            item.getUUID(),
                            item.getX(),
                            item.getY(),
                            item.getZ(),
                            item.getDeltaMovement().x,
                            item.getDeltaMovement().y,
                            item.getDeltaMovement().z));
        }
        return new ItemUpdatePayload(entries);
    }
}
