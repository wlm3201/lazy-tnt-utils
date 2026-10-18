package com.lazytntutils.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.lazytntutils.network.ItemUpdatePayload;

/** 客户端收到的物品实体权威状态缓存，供 ItemEntityMixin 在每个客户端 tick 把本地物品修正为权威状态。 */
public final class ClientItemStorage {

  private ClientItemStorage() {}

  private static final Map<UUID, ItemState> STATE = new ConcurrentHashMap<>();

  public static void updateAll(List<ItemUpdatePayload.ItemEntry> entries) {
    Map<UUID, ItemState> next = new HashMap<>(entries.size());
    for (ItemUpdatePayload.ItemEntry e : entries) {
      next.put(e.uuid(), new ItemState(e.x(), e.y(), e.z(), e.vx(), e.vy(), e.vz()));
    }
    STATE.clear();
    STATE.putAll(next);
  }

  public static ItemState get(UUID uuid) {
    return STATE.get(uuid);
  }

  public static void clear() {
    STATE.clear();
  }

  public static final class ItemState {
    public final double x, y, z;
    public final double vx, vy, vz;

    ItemState(double x, double y, double z, double vx, double vy, double vz) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.vx = vx;
      this.vy = vy;
      this.vz = vz;
    }
  }
}
