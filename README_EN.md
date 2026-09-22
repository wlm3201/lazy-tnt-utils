# Lazy TNT Utils

**English** | [简体中文](README.md)

A Fabric mod for _Minecraft_ that shows the true state of TNT and item entities in lazy chunks, aimed mainly at developing lazy-chunk TNT contraptions. Supports 26.1.2 and 26.2.

---

## The Problem

In a lazy chunk the server does not tick entities, so primed TNT neither counts down its fuse nor moves on its own — but the client keeps simulating it locally. As a result, the client runs the fuse down to zero, making the TNT explode early and disappear.

---

## How It Works

This is a client-side mod; installing it on the server is optional:

- **Client**: in singleplayer, supported entities are corrected every tick to match the server's true state; in multiplayer, client-side local simulation is cancelled, so TNT never explodes early or moves on its own.

- **Server**: every tick the server samples the true state (position / velocity / fuse) of entities within 160 blocks of a player and sends it to that player through a custom network packet; the client caches the data and corrects its local entities. Disabled by default.

### Supported Entity Categories

| Category        | Command name   | Entities covered                                                                                                                                           |
| --------------- | -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| TNT             | `tnt`          | `PrimedTnt`                                                                                                                                                |
| Items           | `item`         | `ItemEntity`                                                                                                                                               |
| Projectiles     | `projectile`   | every subclass of `Projectile` (arrow / trident / snowball / ender pearl / potion / fireball / wind charge / shulker bullet / firework rocket / fishing bobber) |
| Falling Blocks  | `fallingblock` | `FallingBlockEntity`                                                                                                                                       |
| Experience Orbs | `xp`           | `ExperienceOrb`                                                                                                                                            |

---

## Features

- ✅ TNT in lazy chunks no longer explodes early or moves on its own.
- ✅ Entities separated from their rendered position in lazy chunks stay in sync.
- ✅ F3+B draws a momentum arrow for TNT.
- ✅ Hide entity facing arrows.
- ✅ Can be toggled with commands.
- ✅ Accurate explosion particles.
- ✅ Disable the TNT flashing and growing visual effect.
- ✅ Show a label with the ticks left before the TNT explodes.
- ✅ Override render distance and simulation distance.
- ✅ Remove the ±10 limit on Motion.

---

## Commands

All toggles are managed by the **client-side command** `/lazytntutils`:

```
/lazytntutils                                                                      # query the config
/lazytntutils <tnt|item|projectile|fallingblock|xp> <client|server> <true|false>   # sync toggles
/lazytntutils <view|sim> [default|<0..32>]                                         # set render / simulation distance
/lazytntutils tnt <visual|timer> [true|false]                                      # toggle visual effects / label
/lazytntutils <tnt|projectile> arrow [arrow|line|off]                              # momentum vector style
/lazytntutils facing [true|false]                                                  # toggle facing arrows
```

- `client` toggles take effect immediately on the client and are written to `config/lazytntutils-client.properties`.
- `server` toggles are handed to the server through a network packet; the server persists them in `config/lazytntutils.properties` and broadcasts them to all clients.

---

## Building (Developers)

The repository is multi-version: there is a single copy of the sources at the root (`src/main` + `src/client`),
and each Minecraft version is a subproject that only holds a `gradle.properties`
(declaring mc / loader / fabric-api / Java version).

```powershell
# Build every version (Windows)
.\gradlew.bat buildAll

# Build a single version
.\gradlew.bat :v26_1_2:build
```

The artifacts are in the corresponding subproject, e.g. `v26_1_2/build/libs/lazytntutils-1.0.3-26.1.2.jar`.

Building requires **Java 25** (see `javaRelease` in each version's `gradle.properties`).

To support a new Minecraft version, copy any `v26_x/gradle.properties` into a new directory, change its 4 values and `include` it in `settings.gradle` — no need to duplicate the sources.

---

## Performance and Known Limits

For the sake of accuracy, synchronization sends **the full state of every supported entity near the player every tick** (about 65 bytes per primed TNT, about 61 bytes for other categories). Order-of-magnitude estimate (per player, at 20 tps):

- A single primed TNT: about **1.3 KB/s**, scaling linearly with the number of players.
- For comparison, vanilla synchronization is delta-based and throttled, costing roughly **1/10 to none** of this mod in the same scenario.
- Synchronization is paused while `/tick sprint` is running.

> Bandwidth only matters on a **dedicated multiplayer server**; singleplayer runs over loopback and has no real network cost.

---

## License

[MIT](LICENSE) © wlm3201
