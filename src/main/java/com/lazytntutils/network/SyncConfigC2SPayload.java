package com.lazytntutils.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：请求设置某个“服务端下发”开关（server 侧）。 仅携带类别名（tnt / item / projectile / fallingblock）与目标值；服务端据此更新
 * SyncConfig、 持久化，并向所有客户端广播最新的 SyncConfigS2CPayload。类别名未知时服务端不改动任何值。
 */
public record SyncConfigC2SPayload(String kind, boolean value) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SyncConfigC2SPayload> ID =
      new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("lazytntutils", "sync_c2s"));

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
