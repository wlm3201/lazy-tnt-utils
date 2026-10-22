package com.lazytntutils.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ItemUpdatePayload(List<ItemEntry> entries) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ItemUpdatePayload> ID =
            new CustomPacketPayload.Type(
                    Identifier.fromNamespaceAndPath("lazytntutils", "item_sync"));

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
