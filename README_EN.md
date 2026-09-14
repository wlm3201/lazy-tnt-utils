# Lazy TNT Utils

**English** | [简体中文](README.md)

A **Fabric** mod that renders TNT and item entities correctly inside **lazy chunks** — no more phantom client-side explosions. Built for lazy-chunk TNT setups and orbital-strike cannon engineering.

Supports **Minecraft 26.1.2** and **26.2**.

> **Terminology.** The Minecraft Wiki names chunk load levels as **Entity Ticking** (≤31), **Block Ticking** (32), **Border** (33) and **Inaccessible** (34+). Entities tick only in **Entity Ticking** chunks. This README uses **lazy chunk** as the Wiki's informal name for a chunk at the **Block Ticking** load level (and sometimes **Border** too), and **weak-loaded** as a shorthand for "loaded but not Entity Ticking".

---

## The problem

In a lazy chunk the server does not tick entities: a primed TNT's fuse stops counting down and items stop falling. The client, however, keeps simulating them locally, which causes:

- **TNT** — the client runs the fuse down to zero, explodes early and despawns the entity while the server is still holding it.
- **Items** — the client moves them on its own, so their rendered position drifts away from the server.

Both make it impossible to measure timing or position inside a weak-loaded machine.

---

## How it works

The mod is **client-side**, with an **optional server-side** component:

- **Client** — in singleplayer it corrects TNT/item state frame-by-frame to the integrated server's authoritative state; on multiplayer it disables the local simulation entirely, so nothing explodes or drifts early. `tnt` is enabled by default, `item` is off.
- **Server** — every tick the server samples the real state (position / velocity / fuse) of TNT and items within 160 blocks of each player and sends it over a custom packet. The client caches it and corrects the local entities. Off by default.

---

## Features

- ✅ TNT in lazy chunks no longer "explodes early and disappears" on the client.
- ✅ Items in lazy chunks no longer "move on their own".
- ✅ Correct rendering position for separated entities.
- ✅ Momentum arrows drawn for TNT when hitboxes are shown (F3+B).
- ✅ Everything toggleable through commands.
- ✅ Explosion particles rendered exactly at the explosion center.
- ✅ Removes TNT white flashing and pre-explosion scaling.
- ✅ Remaining-fuse tick counter above each TNT.
- ✅ Render & simulation distance override.
- ✅ Momentum vector drawn as arrow, line only, or off.
- ✅ Removes the vanilla ±10 cap on `Motion` (fast movements set via `/summon` / `/data` are no longer zeroed).

---

## Commands

All toggles live in the **client command** `/lazytntutils`:

```
/lazytntutils                                              # show config
/lazytntutils <tnt|item> <client|server> <true|false>      # toggle sync
/lazytntutils <view|sim> [default|<0..32>]                 # render / simulation distance
/lazytntutils tnt <visual|timer> [true|false]              # visual effects / tick label
/lazytntutils tnt arrow [arrow|line|off]                   # momentum vector style
```

- `client` toggles apply instantly on the local client and are stored in `config/lazytntutils-client.properties`.
- `server` toggles are sent to the server, which persists them to `config/lazytntutils.properties` and broadcasts the new state to every client.
- `view` / `sim` are runtime-only overrides written to no config file: restarting the server (or leaving the world in singleplayer) restores the vanilla settings, so nothing competes with `server.properties` as the source of truth.

---

## Building (developers)

All 26.x versions use the official Mojang names. The source is maintained **only once**, in the repository root `src/main`; each MC version switches dependencies through its own subproject `gradle.properties`. Current subprojects: `v26_1_2` (26.1.2) and `v26_2` (26.2).

```powershell
# Build every version (Windows)
.\gradlew.bat buildAll

# Build every version (Linux / macOS)
./gradlew buildAll

# Build a single version
.\gradlew.bat :v26_1_2:build
```

Artifacts land in the matching subproject, e.g. `v26_1_2/build/libs/lazytntutils-26.1.2.jar`.

**Adding / switching a version**: add `include 'vXX'` to `settings.gradle` and create `vXX/gradle.properties` (copy an existing subproject and change only `mc`, `loader`, `fapi`, `jarName` and, if needed, `javaRelease`).

Building requires **Java 25** (see `javaRelease` in each `gradle.properties`).

---

## Performance & known limitations

For accuracy the sync sends **every TNT / item near the player every tick** (≈65 bytes per TNT, ≈61 bytes per item). Rough estimate per player at 20 TPS:

- A single TNT: ≈**1.3 KB/s**, scaling linearly with more players.
- For comparison, vanilla sync is relative + throttled — roughly **1/10** of this in the same scene.
- Sync is paused while `/tick sprint` is running.

> Bandwidth only matters on **dedicated multiplayer servers**; in singleplayer the traffic stays on the loopback interface.

---

## License

[MIT](LICENSE) © wlm3201
