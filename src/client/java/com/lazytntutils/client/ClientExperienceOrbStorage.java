package com.lazytntutils.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.lazytntutils.network.ExperienceOrbUpdatePayload;

/**
 * 客户端收到的经验球权威状态缓存，供 ClientEntityFreezeMixin 在每个客户端 tick 把本地经验球修正为权威状态。
 *
 * <p>经验球的客户端 tick 同样在跑运动（重力 + 靠向附近玩家），弱加载区里服务端不 tick、客户端却一直漂， 与物品完全同构，故修正量也一致（只需 pos / motion）。
 */
public final class ClientExperienceOrbStorage {

  private ClientExperienceOrbStorage() {}

  private static final Map<UUID, MotionState> STATE = new ConcurrentHashMap<>();

  public static void updateAll(List<ExperienceOrbUpdatePayload.Entry> entries) {
    Map<UUID, MotionState> next = new HashMap<>(entries.size());
    for (ExperienceOrbUpdatePayload.Entry e : entries) {
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
