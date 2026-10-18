package com.lazytntutils.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.lazytntutils.network.FallingBlockUpdatePayload;

/**
 * 客户端收到的下落方块（{@link net.minecraft.world.entity.item.FallingBlockEntity}）权威状态缓存。
 *
 * <p>下落方块是弱加载区里除 TNT / 物品之外最常见的一类"客户端自行下落"实体： 服务端不 tick 它就不动，客户端却照常重力下落，于是沙子 / 混凝土 / 铁砧
 * 看着一路穿地。修正方式与物品一致（只需 pos / motion）。
 */
public final class ClientFallingBlockStorage {

  private ClientFallingBlockStorage() {}

  private static final Map<UUID, MotionState> STATE = new ConcurrentHashMap<>();

  public static void updateAll(List<FallingBlockUpdatePayload.Entry> entries) {
    Map<UUID, MotionState> next = new HashMap<>(entries.size());
    for (FallingBlockUpdatePayload.Entry e : entries) {
      next.put(e.uuid(), new MotionState(e.x(), e.y(), e.z(), e.vx(), e.vy(), e.vz()));
    }
    STATE.clear();
    STATE.putAll(next);
  }

  public static MotionState get(UUID uuid) {
    return STATE.get(uuid);
  }

  public static void clear() {
    STATE.clear();
  }
}
