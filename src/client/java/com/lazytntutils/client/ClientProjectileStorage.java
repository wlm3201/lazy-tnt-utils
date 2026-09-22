package com.lazytntutils.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.lazytntutils.network.ProjectileUpdatePayload;

/**
 * 客户端收到的弹射物权威状态缓存，供 ClientEntityFreezeMixin 在每个客户端 tick 把本地弹射物修正为权威状态。
 *
 * <p>覆盖 {@link net.minecraft.world.entity.projectile.Projectile} 的全部子类（箭 / 光灵箭 / 三叉戟 / 雪球 / 鸡蛋 /
 * 末影珍珠 / 药水 / 附魔之瓶 / 各种火球 / 风弹 / 羊驼唾沫 / 潜影弹 / 烟花 / 钓鱼钩）以及原版没继承 Projectile 但同属弹射物的末影之眼与唤魔者尖牙，判定见
 * {@link com.lazytntutils.Projectiles}。
 */
public final class ClientProjectileStorage {

  private ClientProjectileStorage() {}

  private static final Map<UUID, MotionState> STATE = new ConcurrentHashMap<>();

  public static void updateAll(List<ProjectileUpdatePayload.Entry> entries) {
    Map<UUID, MotionState> next = new HashMap<>(entries.size());
    for (ProjectileUpdatePayload.Entry e : entries) {
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
