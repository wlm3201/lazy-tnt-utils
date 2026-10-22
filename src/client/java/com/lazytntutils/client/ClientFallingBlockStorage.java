package com.lazytntutils.client;

import com.lazytntutils.network.FallingBlockUpdatePayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
