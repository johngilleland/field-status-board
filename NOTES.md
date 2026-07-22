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