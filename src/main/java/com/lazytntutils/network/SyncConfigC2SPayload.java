package com.lazytntutils.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncConfigC2SPayload(String kind, boolean value) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncConfigC2SPayload> ID =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("lazytntutils", "sync_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigC2SPayload> CODEC =
            StreamCodec.of(SyncConfigC2SPayload::encode, SyncConfigC2SPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncConfigC2SPayload p) {
        buf.writeUtf(p.kind);
        buf.writeBoolean(p.value);
    }

    private static SyncConfigC2SPayload decode(RegistryFriendlyByteBuf buf) {
        return new SyncConfigC2SPayload(buf.readUtf(), buf.readBoolean());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
