package net.orbitalstrike.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 客户端 → 服务端：请求设置视距 / 模拟距离的强制覆盖值。
 *
 * <p>kind 为 "view" 或 "sim"；value 为 0..32 的距离，或 -1 表示"不覆盖（退回原版设置）"。 服务端据此更新
 * SyncConfig、持久化，并向所有客户端广播新的 SyncConfigS2CPayload。
 *
 * <p>与开关类配置分开成一个包，是因为这里携带的是整数距离而非布尔开关。
 */
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
