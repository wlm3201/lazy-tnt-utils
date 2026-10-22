package com.lazytntutils.server;

import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.TntUpdatePayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.entity.EntityTypeTest;

public final class ServerTntTracker extends EntitySyncTracker<PrimedTnt> {

    private static final ServerTntTracker INSTANCE = new ServerTntTracker();

    private ServerTntTracker() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(INSTANCE::onTick);
    }

    @Override
    protected boolean isEnabled() {
        return SyncConfig.tntServerSync;
    }

    @Override
    protected EntityTypeTest<Entity, PrimedTnt> typeTest() {
        return EntityTypeTest.forClass(PrimedTnt.class);
    }

    @Override
    protected CustomPacketPayload buildPayload(List<PrimedTnt> entities) {
        List<TntUpdatePayload.TntEntry> entries = new ArrayList<>(entities.size());
        for (PrimedTnt tnt : entities) {
            entries.add(
                    new TntUpdatePayload.TntEntry(
                            tnt.getUUID(),
                            tnt.getX(),
                            tnt.getY(),
                            tnt.getZ(),
                            tnt.getDeltaMovement().x,
                            tnt.getDeltaMovement().y,
                            tnt.getDeltaMovement().z,
                            tnt.getFuse()));
        }
        return new TntUpdatePayload(entries);
    }
}
