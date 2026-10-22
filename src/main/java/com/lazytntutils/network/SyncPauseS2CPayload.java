package com.lazytntutils.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncPauseS2CPayload(boolean paused) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncPauseS2CPayload> ID =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("lazytntutils", "sync_pause_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPauseS2CPayload> CODEC =
            StreamCodec.of(SyncPauseS2CPayload::encode, SyncPauseS2CPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncPauseS2CPayload p) {
        buf.writeBoolean(p.paused);
    }

    private static SyncPauseS2CPayload decode(RegistryFriendlyByteBuf buf) {
        return new SyncPauseS2CPayload(buf.readBoolean());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
