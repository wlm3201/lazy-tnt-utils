package com.lazytntutils.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 服务端 → 客户端：通知「tick sprint 期间暂停同步」。
 *
 * <p>/tick sprint 会让服务端以远超 20 TPS 的速度跑 tick，逐 tick 全量下发会把带宽与 客户端压垮。此时服务端停发（见
 * TickSprintGuard），但客户端若继续套用 ClientTntStorage / ClientItemStorage 里的缓存，就会把实体一遍遍纠回 sprint 前的
 * 陈旧位置，表现反而比原版更糟。故必须显式通知客户端一并停手。
 *
 * <p>本包只表达「暂停/恢复」，不改动任何开关值：sprint 结束后服务端照原样恢复， 原本没开同步的玩家不会被误开启。
 */
public record SyncPauseS2CPayload(boolean paused) implements CustomPacketPayload {

  public static final CustomPacketPayload.Type<SyncPauseS2CPayload> ID =
      new CustomPacketPayload.Type<>(
          Identifier.fromNamespaceAndPath("lazytntutils", "sync_pause_s2c"));

  public static final StreamCodec<RegistryFriendlyByteBuf, SyncPauseS2CPayload> CODEC =
      StreamCodec.of(SyncPauseS2CPayload::encode, SyncPauseS2CPayload::decode);

  private static void encode(RegistryFriendlyByteBuf buf, SyncPauseS2CPayload p) {
    buf.writeBoolean(p.paused);
  }

  private static SyncPauseS2CPayload decode(RegistryFriendlyByteBuf buf) {
    return new SyncPauseS2CPayload(buf.readBoolean());
  }

  @Override
  public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
    return ID;
  }
}
