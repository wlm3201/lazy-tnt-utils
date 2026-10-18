package com.lazytntutils.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端 的 TNT 权威状态同步包。
 *
 * <p>包含每颗 TNT 的 UUID、位置、速度（motion 矢量）与引信， 客户端据此按 tick 精确修正本地 TNT，使多人下也能看到与单人一致的逐帧精确运动 （实体分离依赖同一
 * tick 内的精确全局 tick 顺序，原版被节流的实体同步包无法满足）。
 */
public record TntUpdatePayload(List<TntEntry> entries) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<TntUpdatePayload> ID =
      new CustomPacketPayload.Type(Identifier.fromNamespaceAndPath("lazytntutils", "tnt_sync"));

  public static final StreamCodec<RegistryFriendlyByteBuf, TntUpdatePayload> CODEC =
      StreamCodec.of(TntUpdatePayload::encode, TntUpdatePayload::decode);

  private static void encode(RegistryFriendlyByteBuf buf, TntUpdatePayload payload) {
    buf.writeVarInt(payload.entries.size());
    for (TntEntry e : payload.entries) {
      buf.writeUUID(e.uuid);
      buf.writeDouble(e.x);
      buf.writeDouble(e.y);
      buf.writeDouble(e.z);
      buf.writeDouble(e.vx);
      buf.writeDouble(e.vy);
      buf.writeDouble(e.vz);
      buf.writeVarInt(e.fuse);
    }
  }

  private static TntUpdatePayload decode(RegistryFriendlyByteBuf buf) {
    int count = buf.readVarInt();
    List<TntEntry> entries = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      entries.add(
          new TntEntry(
              buf.readUUID(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readVarInt()));
    }
    return new TntUpdatePayload(entries);
  }

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return ID;
  }

  public record TntEntry(
      UUID uuid, double x, double y, double z, double vx, double vy, double vz, int fuse) {}
}
