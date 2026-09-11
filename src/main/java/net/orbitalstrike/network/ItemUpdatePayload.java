package net.orbitalstrike.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端 的物品实体权威状态同步包（与 TntUpdatePayload 平行）。
 *
 * <p>仅同步位置/速度两项：这正是弱加载下客户端“自行下落/漂移”渲染错位的来源； 物品的消失(age)与拾取(pickupDelay)均由服务端权威、通过移除实体包驱动，
 * 客户端不模拟就不会出错，故无需同步。
 */
public record ItemUpdatePayload(List<ItemEntry> entries) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<ItemUpdatePayload> ID =
      new CustomPacketPayload.Type(Identifier.fromNamespaceAndPath("lazytntutils", "item_sync"));

  public static final StreamCodec<RegistryFriendlyByteBuf, ItemUpdatePayload> CODEC =
      StreamCodec.of(ItemUpdatePayload::encode, ItemUpdatePayload::decode);

  private static void encode(RegistryFriendlyByteBuf buf, ItemUpdatePayload payload) {
    buf.writeVarInt(payload.entries.size());
    for (ItemEntry e : payload.entries) {
      buf.writeUUID(e.uuid);
      buf.writeDouble(e.x);
      buf.writeDouble(e.y);
      buf.writeDouble(e.z);
      buf.writeDouble(e.vx);
      buf.writeDouble(e.vy);
      buf.writeDouble(e.vz);
    }
  }

  private static ItemUpdatePayload decode(RegistryFriendlyByteBuf buf) {
    int count = buf.readVarInt();
    List<ItemEntry> entries = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      entries.add(
          new ItemEntry(
              buf.readUUID(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble(),
              buf.readDouble()));
    }
    return new ItemUpdatePayload(entries);
  }

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return ID;
  }

  public record ItemEntry(
      UUID uuid, double x, double y, double z, double vx, double vy, double vz) {}
}
