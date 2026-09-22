package com.lazytntutils.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端 的经验球权威状态同步包（与 ItemUpdatePayload 平行）。
 *
 * <p>只同步位置 / 速度两项：经验球的合并、被玩家吸取、到期消失全部由服务端权威、通过实体变更包驱动， 客户端不本地模拟就不会出错，故无需同步。
 *
 * <p>注意经验球数量可能很大（一次大爆炸就是几百个），本类别默认关闭且只在需要时开。
 */
public record ExperienceOrbUpdatePayload(List<ExperienceOrbUpdatePayload.Entry> entries)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<ExperienceOrbUpdatePayload> ID =
      new CustomPacketPayload.Type(
          Identifier.fromNamespaceAndPath("lazytntutils", "experience_orb_sync"));

  public static final StreamCodec<RegistryFriendlyByteBuf, ExperienceOrbUpdatePayload> CODEC =
      StreamCodec.of(ExperienceOrbUpdatePayload::encode, ExperienceOrbUpdatePayload::decode);

  private static void encode(RegistryFriendlyByteBuf buf, ExperienceOrbUpdatePayload payload) {
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

  private static ExperienceOrbUpdatePayload decode(RegistryFriendlyByteBuf buf) {
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
    return new ExperienceOrbUpdatePayload(entries);
  }

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return ID;
  }

  public record Entry(UUID uuid, double x, double y, double z, double vx, double vy, double vz) {}
}
