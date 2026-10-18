# Lazy TNT Utils

**English** | [简体中文](README.md)

A Fabric mod for _Minecraft_ that shows the true state of TNT and item entities in lazy chunks, aimed mainly at developing lazy-chunk TNT contraptions. Supports 26.1.2, 26.2 and 26.3.

## How It Works

In a lazy chunk the server does not tick entities, so primed TNT neither counts down its fuse nor moves on its own — but the client keeps simulating it locally. As a result, the client runs the fuse down to zero, making the TNT explode early and disappear.

### This mod:

- **Client**: in singleplayer, supported entities are corrected to the integrated server's true state every tick; in multiplayer, client-side local simulation is cancelled, so TNT never explodes early or moves on its own.

- **Server**: every tick the server samples the true state (position / velocity / fuse) of entities within 160 blocks of a player and sends it to that player through a custom network packet; the client caches the data and corrects its local entities. Disabled by default.

## Features

This is a client-side mod; installing it on the server is optional. It is configured with YACL:

- ✅ TNT in lazy chunks no longer explodes early or moves on its own.
- ✅ Entities separated from their rendered position in lazy chunks stay in sync.
- ✅ F3+B draws a momentum arrow for TNT.
- ✅ Hide entity facing arrows.
- ✅ Can be toggled in the config screen (ModMenu + YACL).
- ✅ Precise explosion particles (can be turned off in the config screen to restore the vanilla burst).
- ✅ Disable the TNT flashing and growing visual effect.
- ✅ Show a label with the ticks left before the TNT explodes.
- ✅ Override render distance and simulation distance.
- ✅ Remove the ±10 limit on Motion.

### Supported Entity Categories

| Category        | Entities covered                                                  |
| --------------- | ----------------------------------------------------------------- |
| TNT             | `PrimedTnt`                                                       |
| Items           | `ItemEntity`                                                      |
| Projectiles     | `Projectile` subclasses (arrow / ender pearl / wind charge, etc.) |
| Falling Blocks  | `FallingBlockEntity`                                              |
| Experience Orbs | `ExperienceOrb`                                                   |

## Performance and Known Limits

Server sync sends the full state of the targeted entities around the player every tick (about 65 bytes per entity).
Order-of-magnitude estimate (per player, at 20 tps):

- A single primed TNT: about 1.3 KB/s, scaling linearly with the number of players.
- For comparison, vanilla synchronization is relative-delta and throttled, costing roughly 1/10 to none of this mod in the same scenario.
- Synchronization is paused while `/tick sprint` is running.

Bandwidth only matters on a dedicated multiplayer server; singleplayer runs over loopback and has no real network cost.
