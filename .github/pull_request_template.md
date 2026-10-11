## What and why

<!-- One or two sentences. Link the issue. -->

Closes #

## How to verify

<!-- Commands, screenshots for UI changes (mobile 360 px). -->

## Learning note

<!-- Required for every PR with code changes (docs/workflow.md §7). -->

docs/learning/NNNN-<slug>.md

## Checklist (docs/architecture-rules.md §13)

- [ ] No imports from other modules' `internal`/`web`, no new module arrows (AR-02–AR-04)
- [ ] Status changes through aggregate methods; state machine tested (AR-10)
- [ ] Transaction on the use case, no network calls inside (AR-12, AR-13)
- [ ] Concurrency protected by a lock + DB constraint, with a race test (AR-14, AR-15, AR-60)
- [ ] Repeated commands produce no duplicate events/notifications (AR-17, AR-48)
- [ ] Public DTOs contain no private fields; leak test updated (AR-29, AR-59)
- [ ] Negative access tests (AR-32, AR-58)
- [ ] Problem Details with a stable `code` (AR-28)
- [ ] New migration (not an edited one), table owner stated, backward compatible (AR-19, AR-20)
- [ ] New business table covered by an RLS policy in `db/migration-demo` (AR-52)
- [ ] No PII/tokens/message text in logs (AR-36)
- [ ] UI works at 360 px; buttons come from `allowedActions` (AR-42, AR-45)
- [ ] OpenAPI spec regenerated (AR-27)
- [ ] ADR added if a decision/rule changed (AR-68)
