# Notes

## DittoQueryResult shape (Day 2 spike)

`store.executeRaw(dql)` returns a `CompletionStage<DittoQueryResult>`.

`DittoQueryResult.getItems()` returns `List<? extends DittoQueryResultItem>`.

Each `DittoQueryResultItem.getJsonString()` returns the document as flat JSON,
`_id` included automatically:

```json
{"_id":"alpha-1","callsign":"ALPHA-1","status":"active"}
```

## Persistence confirmed

Ran the app twice against the same local store. First run inserted `alpha-1` successfully. Second run's identical `INSERT` failed with:

> DQL evaluation error: Identifier conflict on document "alpha-1": using FAIL conflict policy.

This confirms the local store genuinely persists across process restarts — if it hadn't, the second run's insert would have succeeded silently instead of conflicting.

## Conflict policy

Plain `INSERT INTO ... DOCUMENTS (...)` defaults to Ditto's **FAIL** conflict policy — it does not upsert by default. Day 3's upsert work needs an explicit conflict-handling clause; TBD what that syntax looks like.

## Tombstone vs. DELETE

Soft-deleting via a `deleted` field update (rather than a real `DELETE`) avoids
a resurrection problem in a CRDT-synced system: if one instance deletes a
document while another instance is offline and still holds a copy, the
offline instance can resurrect the deleted document when it reconnects and
syncs its version back. A tombstone field update instead merges like any
other field change, so the "this is gone" state propagates safely instead of
racing against an offline peer's stale copy.

## Missing vs. NULL fields in DQL

Confirmed via docs: a field that doesn't exist on a document evaluates to
`MISSING` in a `WHERE` clause, which is treated as falsy — `WHERE NOT deleted`
would silently exclude documents that never had a `deleted` field at all, not
include them. `UnitStatus.toDocument()` always writes `deleted: false`
explicitly on every insert/update specifically to avoid this trap.

## EVICT is local-only

`EVICT` removes a document from the *local* device's store only — it does not
sync or propagate. Per Ditto's docs, evicted data reappears if it still
exists on any other connected peer. This is why tombstoning (an `UPDATE`,
which does sync) is the real distributed "this is gone" mechanism, and
`evict` is a separate, unrelated local disk-space concern.

## Local mesh discovery is on by default

Ditto's SDK automatically attempts LAN/mDNS peer discovery alongside its
cloud connection — two instances on the same machine will find and connect
to each other directly over TCP, in addition to each independently talking
to Ditto Cloud. This isn't documented as opt-in; it's on unless explicitly
disabled.

This matters because this project's own scope (see README roadmap) puts
P2P/LAN mesh transports under Stage 2+, not Stage 1 — so for now, the local
mesh connection is disabled explicitly to keep testing isolated to the
intended cloud-mediated path:

```java
ditto.updateTransportConfig(transportConfig -> {
    transportConfig.peerToPeer(p2p -> {
        p2p.lan(lan -> lan.isEnabled(false));
    });
});
```

## `registerObserver` is not the same as `registerSubscription`

This was the real blocker behind Day 4's two-instance sync — easy to miss
since both sound like "watch for changes."

- `DittoStore.registerObserver(query, listener)` — purely local. Fires a
  callback whenever the *local* store changes, regardless of source. It
  does **not** tell Ditto's sync engine to fetch anything from peers/cloud.
- `DittoSync.registerSubscription(query)` — tells the sync engine "replicate
  documents matching this query from other peers into my local store."
  Without this, an instance can be fully connected and authenticated and
  still never receive a single document from another peer.

Both are needed together for a reactive board driven by remote data: the
subscription pulls the data in, the observer notifies the app once it's
there.

## Writes need time to actually leave the process

Getting a completed `CompletionStage` back from `execute()` only confirms
the write succeeded *locally* — it does not mean the write has already been
pushed out over the network. Replication to peers/cloud happens
asynchronously on Ditto's own schedule. In early two-instance testing, the
writer process exited (~9s total runtime) before its own write had time to
leave the machine, so the listener never received it — not a sync failure,
just a process lifetime too short for the write to propagate.

## Partition demo result

Two instances, each independently disconnected from Ditto Cloud
(`ditto.getSync().stop()`), made concurrent edits to *different fields* of
the same document (`status` on one, `lastTelemetryTick` on the other) while
offline, then reconnected (`ditto.getSync().start()`). Both instances
converged to an identical final document containing both edits — confirmed
via matching output on both sides. No manual conflict resolution;
Ditto's CRDT merge combined the field-level changes automatically.