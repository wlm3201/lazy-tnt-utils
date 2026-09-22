package com.lazytntutils.network;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 网络包编解码注册助手。
 *
 * <p>集成服务端（runClient，单进程同时跑服务端与客户端的 onInitialize）会对同一方向的包 重复注册而抛 “already
 * registered”。这里用进程级集合去重，保证每个包只注册一次， 且单进程 / 纯客户端 / 纯服务端 三种环境下都能正确收发。
 */
public final class Networking {

  private Networking() {}

  private static final Set<CustomPacketPayload.Type<?>> REGISTERED = ConcurrentHashMap.newKeySet();

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
