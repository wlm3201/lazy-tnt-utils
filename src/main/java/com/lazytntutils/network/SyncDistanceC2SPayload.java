package com.lazytntutils.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncDistanceC2SPayload(String kind, int value) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncDistanceC2SPayload> ID =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("lazytntutils", "sync_distance_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDistanceC2SPayload> CODEC =
            StreamCodec.of(SyncDistanceC2SPayload::encode, SyncDistanceC2SPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncDistanceC2SPayload p) {
        buf.writeUtf(p.kind);
        buf.writeVarInt(p.value);
    }

    private static SyncDistanceC2SPayload decode(RegistryFriendlyByteBuf buf) {
        return new SyncDistanceC2SPayload(buf.readUtf(), buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
