# 0006. UUIDv7 identifiers

- **Status:** accepted
- **Date:** 2026-10-11
- **Affects:** all modules, migrations; AR-21

## Context

Entity IDs appear in public URLs (listing cards). Sequential IDs leak volumes and allow enumeration. Random UUIDv4 fragment B-tree indexes and are not sortable by time.

## Decision

- All entity IDs are UUIDv7, stored as `uuid`.
- IDs are generated in the application before persist, so the ID is known before the transaction commits (events, outbox, idempotency keys).
- Generation is wrapped in one `shared` component (`IdGenerator`), so the library can be replaced and tests can use deterministic IDs.

## Alternatives

- **`bigint` sequences.** Compact, but enumerable and leak volumes; a separate public slug would be needed anyway.
- **UUIDv4.** No ordering, worse index locality.
- **DB-side `uuidv7()` (PostgreSQL 18).** Ties ID creation to the insert; the ID is unknown before persist.

## Consequences

- UUIDv7 reveals the creation time with millisecond precision. This is acceptable: the publication date is public anyway.
- Cursor pagination can use the ID as a tiebreaker in time order.
