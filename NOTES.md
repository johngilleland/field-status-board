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
