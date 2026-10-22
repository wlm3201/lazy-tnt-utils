package com.lazytntutils.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncConfigS2CPayload(
        boolean tntServerSync,
        boolean itemServerSync,
        boolean projectileServerSync,
        boolean fallingBlockServerSync,
        boolean experienceOrbServerSync,
        int viewDistanceOverride,
        int simulationDistanceOverride)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncConfigS2CPayload> ID =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("lazytntutils", "sync_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigS2CPayload> CODEC =
            StreamCodec.of(SyncConfigS2CPayload::encode, SyncConfigS2CPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncConfigS2CPayload p) {
        buf.writeBoolean(p.tntServerSync);
        buf.writeBoolean(p.itemServerSync);
        buf.writeBoolean(p.projectileServerSync);
        buf.writeBoolean(p.fallingBlockServerSync);
        buf.writeBoolean(p.experienceOrbServerSync);
        buf.writeVarInt(p.viewDistanceOverride);
        buf.writeVarInt(p.simulationDistanceOverride);
    }

    private static SyncConfigS2CPayload decode(RegistryFriendlyByteBuf buf) {
        boolean tnt = buf.readBoolean();
        boolean item = buf.readBoolean();
        boolean projectile = buf.readBoolean();
        boolean fallingBlock = buf.readBoolean();
        boolean experienceOrb = buf.readBoolean();
        int view = buf.readVarInt();
        int sim = buf.readVarInt();
        return new SyncConfigS2CPayload(
                tnt, item, projectile, fallingBlock, experienceOrb, view, sim);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
