package com.lazytntutils.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端 的下落方块权威状态同步包（与 ItemUpdatePayload 平行）。
 *
 * <p>只同步位置 / 速度两项：下落方块何时落地成块、是否砸坏方块都由服务端权威、 通过实体移除包驱动，客户端不本地模拟就不会出错，故无需同步。
 */
public record FallingBlockUpdatePayload(List<FallingBlockUpdatePayload.Entry> entries)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<FallingBlockUpdatePayload> ID =
      new CustomPacketPayload.Type(
          Identifier.fromNamespaceAndPath("lazytntutils", "falling_block_sync"));

  public static final StreamCodec<RegistryFriendlyByteBuf, FallingBlockUpdatePayload> CODEC =
      StreamCodec.of(FallingBlockUpdatePayload::encode, FallingBlockUpdatePayload::decode);

  private static void encode(RegistryFriendlyByteBuf buf, FallingBlockUpdatePayload payload) {
    buf.writeVarInt(payload.entries.size());
    for (Entry e : payload.entries) {
      buf.writeUUID(e.uuid);
      buf.writeDouble(e.x);
      buf.writeDouble(e.y);
      buf.writeDouble(e.z);
      buf.writeDouble(e.vx);
      buf.writeDouble(e.vy);
      buf.writeDouble(e.vz);
    }
  }

  private static FallingBlockUpdatePayload decode(RegistryFriendlyByteBuf buf) {
    int count = buf.readVarInt();
    List<Entry> entries = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      entries.add(
          new Entry(
              buf.readUUID(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble()));
    }
    return new FallingBlockUpdatePayload(entries);
  }

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return ID;
  }

  public record Entry(UUID uuid, double x, double y, double z, double vx, double vy, double vz) {}
}
