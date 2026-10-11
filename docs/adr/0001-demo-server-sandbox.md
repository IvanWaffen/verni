# 0001. Demo mode is a server sandbox

- **Status:** accepted
- **Date:** 2026-10-11
- **Affects:** `demo` module, infrastructure, AR-50…AR-55; BR-09, E2E-06, TECH-10

## Context

The demo exists for the portfolio. Within 2–3 minutes a visitor without an account walks through "lost → found → two claimants → returned". The demo must not touch real data (TECH-10). At the same time it should show the real backend at work, including the concurrency invariants.

## Decision

- A separate deployment of the same code with the `demo` profile on `demo.<domain>`. It has its own DB, its own S3 bucket and its own credentials, with no access to production resources.
- Each visitor gets a sandbox (`sandbox_id`). The story is seeded through the modules' public commands.
- Sandboxes are isolated from each other with PostgreSQL Row-Level Security. The `sandbox_id` column and the policies are added by demo migrations (`db/migration-demo`). Business code does not know about them; the backend runs `SET LOCAL verni.sandbox_id` per transaction.
- Login is a persona choice, no email. Email goes to a built-in `DemoMailbox`.
- Sandbox TTL is 24 h, creation is rate-limited per IP, with a global cap on active sandboxes.

## Alternatives

- **Demo on frontend fixtures.** Simpler and safer, but races and invariants are not real, which loses the main point for the portfolio.
- **Demo data in the production DB with an `is_demo` flag.** Isolation would rely on filters in every query; one mistake means a leak. Rejected.
- **A separate DB per visitor.** Maximum isolation, but unacceptable cost and creation time.

## Consequences

- Every new business table needs an RLS policy in `db/migration-demo` (a review checklist item).
- A test is required: sandbox A cannot see sandbox B's data through any endpoint.
- Production starts with `verni.demo.enabled=false`. If demo mode is enabled together with the production profile, the app fails on startup.
- Extra resources: a second backend/frontend container and a demo DB (can live on the same PostgreSQL server with a separate role and database).
- The demo DB and the production DB receive the same main migrations, so their schemas do not diverge.
