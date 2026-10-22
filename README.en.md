# Lazy TNT Utils

English | [简体中文](README.md)

A Minecraft Fabric mod that shows the true state of TNT and item entities in lazy chunks, aimed at
developing lazy-chunk TNT contraptions. Supports 26.1.2, 26.2 and 26.3.

## How it works

In lazy chunks the server does not tick entities, so TNT neither counts down its fuse nor moves on
its own — but the client keeps simulating locally, runs the fuse out, and detonates the TNT early.

### What this mod does

- **Client**: in single player, corrects supported entities to the integrated server's real state
  every tick; in multiplayer, cancels the client-side local simulation, so TNT neither detonates
  early nor drifts.
- **Server**: every tick, collects the real state (position / velocity / fuse) of entities within
  160 blocks of a player and sends it to that player over a custom packet; the client caches it and
  corrects its local entities. Off by default.

## Features

Client-side, with an optional server-side component, configured through YACL:

- ✅ TNT in lazy chunks no longer detonates early or drifts
- ✅ Entity render positions in lazy chunks stay in sync
- ✅ F3+B draws motion arrows for TNT
- ✅ Hide entity facing arrows
- ✅ Toggle everything from the config screen (ModMenu + YACL)
- ✅ Precise explosion particles (can be disabled to fall back to the vanilla random blob)
- ✅ Disable the TNT flashing and scaling effect
- ✅ Show a label with the ticks left before detonation
- ✅ Adjust render distance and simulation distance
- ✅ Lift the ±10 clamp on Motion

### Supported entity categories

| Category | Entities covered |
| --- | --- |
| TNT | `PrimedTnt` |
| Items | `ItemEntity` |
| Projectiles | `Projectile` subclasses (arrows / ender pearls / wind charges, …) |
| Falling blocks | `FallingBlockEntity` |
| Experience | `ExperienceOrb` |

## Performance and known limits

The server sends the full state of nearby entities every tick (about 65 bytes per entity).

Order of magnitude per player at 20 tps:

- A single TNT: roughly 1.3 KB/s, scaling linearly with more players.
- For comparison, vanilla sync is delta-based and throttled — around 1/10 of this mod down to zero
  in the same scenario.
- Sync pauses during a tick sprint.

Bandwidth only matters on a dedicated server; single player goes over loopback and costs nothing.
