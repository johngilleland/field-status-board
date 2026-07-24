# Field Status Board

## Purpose

A CLI app for tracking field unit status - callsign, status, telemetry - synced in real time between instances via [Ditto](https://ditto.live). The core functionality is the ability of two disconnected instances to take concurrent edits to the same unit, and converge cleanly on reconnect without a central server arbitrating conflicts.

## Architecture (sketch)

```
+------------------+
| Instance A (CLI) |
| Repository + DQL |
+---------+--------+
          |
          | Ditto sync
          | (cloud or P2P)
          |
+---------+--------+
| Instance B (CLI) |
| Repository + DQL |
+------------------+
```

Each instance holds its own local Ditto store and stays functional offline. Sync is peer-to-peer/cloud-mediated via the Ditto SDK - no custom server. Full diagram with the partition/merge sequence to be developed after SDK integration.

## Roadmap

**Stage 1 (MVP - v0.1.0)**
- CLI scaffold, CI, toolchain verified
- Ditto SDK integration, auth, local read/write
- 'UnitStatus' domain model + repository (upsert, reads, targeted updates, tombstone/evict)
- Live board driven by local store observer
- Two-instance sync, partition/reconnect convergence demo
- Full CLI command loop ('report' / 'status' / 'tick' / 'list' / 'remove' / 'help')

**Stage 2+ (MMP - v0.2.0 - v0.3.0)**
- P2P transports (LAN mesh, no cloud dependency)
- Narrowed subscription scopes
- Explicit eviction policy
- Sync status monitoring
- Degraded-network testing with 'tc'/'netem'
- Web dashboard

## Demo: Partition and Reconnect

Proves two instances can each take concurrent edits to *different fields*
of the same document while fully disconnected, and converge to a single
merged state on reconnect — with zero manual conflict resolution.

**Setup:** two terminals, same project directory.

**Terminal 1:**
```bash
./gradlew run --args="a"
```

**Terminal 2** (start within a second or two of terminal 1):
```bash
./gradlew run --args="b"
```

**What happens:** each instance connects, then deliberately disconnects
from Ditto Cloud (`ditto.getSync().stop()`), simulating a network
partition. While disconnected, instance A edits the unit's `status`
field; instance B edits its `lastTelemetryTick` field — concurrent,
conflicting-looking edits to the same document, made with no
coordination between the two. Both instances then reconnect
(`ditto.getSync().start()`).

**Observed result:** both instances converge to an identical final
document containing *both* edits — A's `status` change and B's
telemetry tick change — printed as each instance's own
`"Final merged state:"` line. Neither edit is lost, and neither instance
had to manually resolve a conflict; Ditto's CRDT merge combined the two
field-level changes automatically.