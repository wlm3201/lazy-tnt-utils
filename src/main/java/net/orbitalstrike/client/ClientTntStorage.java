package net.orbitalstrike.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.orbitalstrike.network.TntUpdatePayload;

/** 客户端收到的 TNT 权威状态缓存，供 TntEntityMixin 在每个客户端 tick 把本地 TNT 修正为权威状态。 */
public final class ClientTntStorage {

  private ClientTntStorage() {}

  private static final Map<UUID, TntState> STATE = new ConcurrentHashMap<>();

  public static void updateAll(List<TntUpdatePayload.TntEntry> entries) {
    Map<UUID, TntState> next = new HashMap<>(entries.size());
    for (TntUpdatePayload.TntEntry e : entries) {
      next.put(e.uuid(), new TntState(e.x(), e.y(), e.z(), e.vx(), e.vy(), e.vz(), e.fuse()));
    }
    STATE.clear();
    STATE.putAll(next);
  }

  public static TntState get(UUID uuid) {
    return STATE.get(uuid);
  }

  public static void clear() {
    STATE.clear();
  }

  public static final class TntState {
    public final double x, y, z;
    public final double vx, vy, vz;
    public final int fuse;

    TntState(double x, double y, double z, double vx, double vy, double vz, int fuse) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.vx = vx;
      this.vy = vy;
      this.vz = vz;
      this.fuse = fuse;
    }
  }
}
