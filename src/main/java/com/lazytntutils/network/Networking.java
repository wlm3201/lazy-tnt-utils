package com.lazytntutils.network;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class Networking {

    private Networking() {}

    private static final Set<CustomPacketPayload.Type<?>> REGISTERED =
            ConcurrentHashMap.newKeySet();

    public static <T extends CustomPacketPayload> void clientbound(
            CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        if (REGISTERED.add(id)) {
            PayloadTypeRegistry.clientboundPlay().register(id, codec);
        }
    }

    public static <T extends CustomPacketPayload> void serverbound(
            CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        if (REGISTERED.add(id)) {
            PayloadTypeRegistry.serverboundPlay().register(id, codec);
        }
    }
}
