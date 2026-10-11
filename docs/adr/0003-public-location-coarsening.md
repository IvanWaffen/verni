# 0003. Public location coarsening

- **Status:** accepted
- **Date:** 2026-10-11
- **Affects:** `listings`, `discovery` modules; AR-24; product principle 5, E2E-04

## Context

The public point must not reveal a home address or the place where a valuable item is kept. If the point is exposed exactly as the user picked it, it may coincide with their home.

## Decision

- On write, the server snaps the point to a grid and stores the result in a separate `public_location` column. Only this column is exposed and included in `discovery_listings_v`.
- Grid step: 500 m by default and 1500 m for categories flagged `sensitive` in the `categories` dictionary (initial list: documents, keys, bank cards).
- Steps are configurable. The public point is recomputed when the category or the point changes.
- Radius search and match suggestions also use **the public point**. The original point is never used in search, otherwise it could be triangulated with a series of queries with different radii. An error within one grid step is acceptable for finding items.
- The original point is visible only to the author (in their own DTO).

## Alternatives

- **Random offset (jitter).** After repeated edits the original point can be recovered from several offsets. Rejected.
- **District/city only, no point.** Breaks the map and geo search.

## Consequences

- Test: the public API does not return original coordinates; the public point lies on a grid with the right step.
- The UI shows an area/circle on the map, not a "right here" pin.
