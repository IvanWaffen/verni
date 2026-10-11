# 0002. Both sides confirm the return

- **Status:** accepted
- **Date:** 2026-10-11
- **Affects:** `responses`, `listings`, `notifications` modules; AR-16; BR-05, E2E-02

## Context

A product principle is a "verifiable outcome". If only the author confirms the return, the "Returned to owner" outcome relies on the word of one side.

## Decision

- A new command `POST /api/v1/responses/{id}/confirm-return` is available to both sides of an agreed handover. It replaces `complete` from the API sketch.
- Confirmations are recorded in `author_confirmed_at` and `responder_confirmed_at`. The response stays in `HANDOVER_AGREED` while only one side has confirmed.
- The second confirmation atomically moves the response to `COMPLETED`, the listing to `CLOSED/RETURNED`, and other open responses to a terminal state.
- `POST /api/v1/responses/{id}/cancel-handover` is available to either side before completion. The response returns to `DISCUSSING` and the handover slot is freed.
- If the other side is silent, a reminder is sent after 3 days. After 14 days the author can complete the return alone; the outcome gets `confirmation = ONE_SIDED`, visible in the history. Both periods are configurable.

## Alternatives

- **Only the author confirms.** Simpler, but a weaker guarantee of an honest outcome.
- **Strict two-party confirmation without a timeout.** A listing could hang forever if the other side disappears.

## Consequences

- The response state machine in the business requirements is updated: the sides' confirmations are separate facts within `HANDOVER_AGREED`, followed by `COMPLETED`.
- Kanban: the "Handover agreed" column shows whose confirmation is pending.
- Tests: a repeated confirmation by the same side is idempotent; parallel confirmations by both sides produce exactly one completion and one event.
