package com.lazytntutils.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端 的弹射物权威状态同步包（与 ItemUpdatePayload 平行）。
 *
 * <p>只同步位置 / 速度两项：弹射物的命中、消失、拾取等全部由服务端权威、通过实体变更包驱动， 客户端不本地模拟就不会出错，故无需同步。
 *
 * <p>覆盖 {@link com.lazytntutils.Projectiles} 判定的全部弹射物（Projectile 子类 + 末影之眼 + 唤魔者尖牙），
 * 因此一个包、一个开关就能管住所有投掷物，不必逐个实体类型加。
 */
public record ProjectileUpdatePayload(List<ProjectileUpdatePayload.Entry> entries)
    implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<ProjectileUpdatePayload> ID =
      new CustomPacketPayload.Type(
          Identifier.fromNamespaceAndPath("lazytntutils", "projectile_sync"));

  public static final StreamCodec<RegistryFriendlyByteBuf, ProjectileUpdatePayload> CODEC =
      StreamCodec.of(ProjectileUpdatePayload::encode, ProjectileUpdatePayload::decode);

  private static void encode(RegistryFriendlyByteBuf buf, ProjectileUpdatePayload payload) {
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

  private static ProjectileUpdatePayload decode(RegistryFriendlyByteBuf buf) {
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
    return new ProjectileUpdatePayload(entries);
  }

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return ID;
  }

  public record Entry(UUID uuid, double x, double y, double z, double vx, double vy, double vz) {}
}
