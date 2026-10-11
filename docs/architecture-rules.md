# VERNI — Architecture Rules for Development

> Mandatory for code, review and CI. Rules are cited as `AR-xx`; changing one requires an ADR (AR-68). **[default]** = chosen to unblock the skeleton, revisable before the module is built.
> Conflict priority: complete scenario → security/privacy → mobile UX → architecture → measured performance.

## 1. Modular monolith: boundaries

**AR-01. One module — one top-level package** `ru.verni.<module>`: `identity`, `listings`, `discovery`, `responses`, `messaging`, `media`, `moderation`, `notifications`, `audit`, `demo` (active only in the demo deployment, §9), plus `shared`.

**AR-02. Public and internal code are separated by packages:**

```text
ru.verni.responses/
├── ResponseCommands.java      // module public API (interfaces, record DTOs, events)
├── ResponseQueries.java
├── HandoverAgreed.java        // domain event (public)
├── web/                       // REST controllers and web DTOs (not used by other modules)
└── internal/                  // domain, application, persistence: NOT accessible to neighbors
    ├── domain/
    ├── application/
    └── persistence/
```

Neighbors use only the module's root package. Everything in `internal`/`web` is private.

**AR-03. The dependency graph is fixed; cycles are forbidden.** A module may call only the APIs of modules listed in its row:

| Module | Allowed dependencies (API) | Listens to events from |
|---|---|---|
| `shared` | — | — |
| `identity` | shared | — |
| `media` | shared | — |
| `listings` | identity, media | — |
| `discovery` | listings | — |
| `responses` | listings, identity | listings |
| `messaging` | responses, identity | responses |
| `moderation` | listings, identity | — |
| `notifications` | identity | all modules |
| `audit` | — | all modules |
| `demo` | identity, listings, responses, messaging | — |

Communication against an arrow (e.g. `listings` → `responses`) happens **only through events**. A new arrow in the table requires an ADR.

**AR-04. Other modules' repositories, entities and tables are off limits.** No imports of a neighbor's entities/repositories, no SQL against its tables. Cross-module references are IDs (`UUID listingId`), never `@ManyToOne`. DB foreign keys between modules are allowed.

**AR-05. The only sanctioned exception is the search read view.** `discovery` reads data through the SQL view `discovery_listings_v`. It is created and versioned by a `listings` migration. The view contains **only publicly allowed fields** (no email, hidden features or exact point). `discovery` writes nothing.

**AR-06. `shared` is not a dumping ground.** It holds only technical things without business meaning: `CurrentUser`, base error classes, `Clock` configuration, ID types, pagination utilities. Business concepts ("listing", "response") are forbidden in `shared`.

**AR-07. Boundaries are verified automatically**: Spring Modulith 2.1 with `ApplicationModules.of(VerniApplication.class).verify()` in CI. The module structure test is mandatory from the first commit. The Modulith event publication registry is not used; durable side effects go through our own outbox (§8).

---

## 2. Layers inside a module

**AR-08. Dependency direction:** `web` → `application` → `domain` ← `persistence`/infrastructure.

| Layer | Contains | Must not |
|---|---|---|
| `web` | Controllers, request/response DTOs, mapping, format validation | Contain business rules, open transactions, expose entities |
| `application` | Use cases (one class/method per command), transactions, permission checks, event publishing | Know about HTTP (`HttpServletRequest`, status codes) |
| `domain` | Aggregates, state machines, invariants, domain events, value objects | Depend on Spring Web, S3, SMTP, `JdbcClient` |
| `persistence` | Spring Data repositories, `JdbcClient` queries, S3/SMTP adapters | Make business decisions |

**AR-09. Pragmatic compromise:** JPA annotations on domain entities are allowed; a separate persistence model is not needed in the MVP. Spring Web and Jackson annotations in `domain` are forbidden.

**AR-10. State transitions happen only through aggregate methods.** There are no public status setters:

```java
response.agreeHandover(actorId, clock);   // checks the state machine and permissions, emits an event
listing.close(CloseReason.RETURNED, actorId, clock);
```

The state machine is defined in one place (an enum with allowed transitions) and covered by a unit test for **every** "state × command" pair.

**AR-11. The server computes Kanban columns and available actions.** `/me/board` returns the column derived from states. Every card/response DTO includes `allowedActions` (e.g. `["AGREE_HANDOVER","REJECT"]`). The frontend **does not duplicate** state machines. Dragging a card in the UI calls the matching command and never changes the "column" directly.

---

## 3. Transactions, consistency, concurrency

**AR-12. One command — one transaction.** `@Transactional` is placed only on use cases in `application`, not on controllers or repositories (except the read-only default).

**AR-13. No network calls inside a transaction** (S3, SMTP, external HTTP). External side effects go through the outbox (§8).

**AR-14. The consistency root for a handover is the `listings` row.** Any command that changes a listing's fate or opens a handover (`agree-handover`, `confirm-return`, `cancel-handover`, `close`, `hide`, creating a response) first locks the listing row through the `listings` API:
- state-changing commands take `SELECT … FOR UPDATE`;
- creating a response takes `FOR SHARE` and checks that the listing is open.

**AR-15. The database backs up the invariants.** At minimum:

```sql
-- at most one agreed handover per listing
CREATE UNIQUE INDEX ux_responses_one_handover
    ON responses (listing_id) WHERE status = 'HANDOVER_AGREED';
-- one open response per user per listing
CREATE UNIQUE INDEX ux_responses_one_open_per_user
    ON responses (listing_id, responder_id) WHERE status IN ('NEW','DISCUSSING','HANDOVER_AGREED');
```

A constraint violation becomes a `409` with a stable error code, not a `500`.

**AR-16. Both sides confirm the return** ([ADR-0002](adr/0002-two-party-return-confirmation.md): `confirm-return`, `cancel-handover`, 3/14-day timeouts). The second confirmation, in one transaction: response → `COMPLETED`, listing → `CLOSED/RETURNED`, other open responses → terminal, audit + outbox. Closing/hiding a listing terminates its open responses in the same transaction (via event).

**AR-17. Repeated commands are idempotent.** If the entity is already in the target state from the same command by the same actor, return `200` with the current state **without new events or notifications**. If the transition is not allowed, return `409`. Concurrent edits use `@Version` (optimistic locking); the version is passed in the DTO.

**AR-18. Synchronous events for writes in the same transaction.** Handlers that write audit and outbox records are `@EventListener` (they run in the command's transaction). `@TransactionalEventListener(AFTER_COMMIT)` is forbidden for critical writes: they are lost on failure.

---

## 4. Data and migrations

**AR-19. Only Flyway manages the schema.** `spring.jpa.hibernate.ddl-auto=validate`. Applied migrations are never edited; fixes are made with a new migration. File name: `V<yyyyMMddHHmm>__<module>_<description>.sql` to avoid collisions between branches.

**AR-20. Table ownership.** Every table has exactly one owner module, stated in the migration (`COMMENT ON TABLE … IS 'owner: listings'`). Only the owner writes to the table.

**AR-21. Identifiers:** UUIDv7 generated in the application before persist ([ADR-0006](adr/0006-uuidv7-identifiers.md)). Sequential IDs are never exposed.

**AR-22. Types.**
- Time is stored as `timestamptz` in UTC; in Java — `Instant`.
- `Clock` is injected as a bean; `Instant.now()` in business code is forbidden.
- The event date (`happened_at`) is stored separately from the publication date.
- Enums are stored as `text` + `CHECK`, not ordinals.

**AR-23. JPA hygiene.**
- `spring.jpa.open-in-view=false`.
- Entities never leave `application`.
- Lists and search are read via projections/`JdbcClient`, without loading entity graphs.
- N+1 is detected by tests/query logs on key lists.

**AR-24. Geodata.**
- Type `geography(Point, 4326)`, GiST index, search via `ST_DWithin` with a mandatory result limit.
- `public_location` = server-side grid snap (500 m; 1500 m for `sensitive` categories), recomputed on change ([ADR-0003](adr/0003-public-location-coarsening.md)). Search, matching and all non-author responses use only it.

**AR-25. Nothing is "physically deleted by default".** Listings hidden by moderators and closed listings stay in the DB for audit (BR-08). Personal data deletion is a separate procedure per the retention policy, not a `DELETE` in business code.

---

## 5. API contract

**AR-26. Prefix and style.** Everything lives under `/api/v1`. Resources are plural nouns. Business commands are `POST /{resource}/{id}/{action}` (`agree-handover`, `confirm-return`, `close`), not `PATCH status`.

**AR-27. OpenAPI is the source of truth for the frontend**, code-first ([ADR-0005](adr/0005-openapi-code-first.md)): controllers and DTOs come first, springdoc generates the spec. The generated spec is committed to `docs/api/openapi.yaml`. CI fails if it is outdated or contains a breaking change without a version bump (oasdiff). The frontend generates types from the spec; hand-written copies of types are forbidden.

**AR-28. Errors use Problem Details** (`application/problem+json`) with a mandatory `code` extension:

| HTTP | When | Example `code` |
|---|---|---|
| 400 | Invalid input (+ `errors[]` per field) | `VALIDATION_FAILED` |
| 401 | No session for a state-changing operation | `UNAUTHENTICATED` |
| 403 | The resource is visible, but the action is not allowed | `NOT_LISTING_AUTHOR` |
| 404 | The resource does not exist **or must not be visible** to the caller (someone else's conversation, a hidden listing) | `NOT_FOUND` |
| 409 | Invalid transition or concurrency conflict | `HANDOVER_ALREADY_AGREED`, `LISTING_CLOSED` |
| 429 | Rate limit exceeded | `RATE_LIMITED` |

Codes are a stable contract; the frontend branches on `code`.

**AR-29. A separate DTO for each audience.** `PublicListingView`, `OwnListingView`, `ModeratorListingView` are different classes. Extending a public DTO with "a field we fill only for the author" is forbidden. This is the main mechanism behind E2E-04.

**AR-30. Paginated lists.** Feed and search use cursor pagination (stable while loading more on a phone). Page size is capped by the server.

---

## 6. Security and privacy

**AR-31. Deny by default.** `SecurityFilterChain` explicitly lists public GETs. Everything else requires authentication.

**AR-32. Permission checks live in `application`, not only in URL rules.** Every use case explicitly calls a policy (`ListingAccessPolicy.requireAuthor(...)`) before changing anything. Every state-changing endpoint has **negative** integration tests: another user, a guest, a blocked user, a moderator outside their rights.

**AR-33. Sessions and CSRF.**
- Server-side sessions are stored in PostgreSQL (Spring Session JDBC) **[default]**, so they survive restarts and need no Redis.
- Cookie: `Secure; HttpOnly; SameSite=Lax`.
- CSRF via a cookie token for all non-GET requests.
- Frontend and API live on one domain; CORS is disabled.

**AR-34. One-time codes and tokens** are stored only as hashes, with a TTL, an attempt limit and invalidation after use. Comparison is constant-time.

**AR-35. Rate limits in two layers:**
- reverse proxy — coarse, per IP;
- application — business limits per user (posts/day, responses/hour, messages/minute), configured via `@ConfigurationProperties`.

**AR-36. Sensitive fields** (email, hidden features, exact point, IP, message text):
- never appear in public DTOs, SEO/OG meta tags, logs, metrics or error texts;
- hidden features are stored in a separate column/table visible only to the author;
- email is masked in logs (`i***@proton.me`).

**AR-37. Photos.**
- Check the file signature (not just MIME/extension) and the size limit.
- The server **re-encodes** the image, thereby stripping EXIF. The original is not stored.
- The S3 key is random; the bucket is private.
- Photos are served via a stable URL `/api/v1/media/{id}`: the server checks the listing's visibility and redirects to a short-lived presigned URL. OG previews work, and a photo hidden by a moderator stops opening.

**AR-38. Secrets** come only from environment variables/CI secrets. CI runs secret scanning; a commit with a secret is blocked.

---

## 7. Frontend (Next.js)

**AR-39. Rendering by data sensitivity.**
- Public pages (feed, card, demo landing) use Server Components/SSR for SEO and OG.
- Personal data (Mine, conversations, moderation) is loaded **only on the client** with the user's cookie. The Next.js server cache never contains personal responses.

**AR-40. One typed API client** (`frontend/src/shared/api`), generated from OpenAPI. Direct `fetch` calls from components are forbidden. Errors are handled by `code` (AR-28).

**AR-41. Feature-based structure:** `src/features/<feature>/` (feature components, hooks, tests) and `src/shared/` (shadcn/ui kit, API client, utilities). A feature does not import another feature's internals.

**AR-42. Business rules are not duplicated.** Button visibility comes from server `allowedActions` (AR-11). Client-side form validation is only for UX; the server always re-validates.

**AR-43. The pre-login draft** ([ADR-0004](adr/0004-client-side-draft.md)) is stored on the client (IndexedDB, including photos) and sent to the server after email confirmation. Anonymous uploads to the server are not accepted (protection against storage abuse).

**AR-44. The map is an optional layer.** MapLibre is loaded dynamically, client-side only. Every map scenario is also available via the feed and filters. A tile provider outage does not break the page.

**AR-45. Mobile-first and accessibility are acceptance criteria, not wishes.**
- Layout from 360 px.
- Interactive elements ≥ 44×44 px.
- Fields are labeled.
- Keyboard focus works.
- Playwright runs on a mobile viewport; axe checks of key pages run in CI.

**AR-46. TypeScript strict**, `any` is forbidden (except isolated adapters with a comment). ESLint + Prettier, `tsc --noEmit` in CI.

---

## 8. Background jobs and notifications

**AR-47. All external side effects go through `outbox_jobs`.** The record is written in the same transaction as the business change (AR-13, AR-18).

**AR-48. Handlers are idempotent.**
- `idempotency_key` is unique in the table, e.g. `notify:new-response:{responseId}`.
- Jobs are claimed with `FOR UPDATE SKIP LOCKED`.
- Exponential backoff; after the attempt limit the job goes to `DEAD` and an alert fires.
- A retry never sends a duplicate.

**AR-49. Periodic jobs** (token and temp file cleanup, sandbox expiry) are safe to run on multiple instances: advisory lock or ShedLock, even while there is only one instance.

---

## 9. Demo mode ([ADR-0001](adr/0001-demo-server-sandbox.md))

**AR-50. Separate deployment** (`demo` profile, own DB, bucket and credentials without access to production). Production fails to start if `verni.demo.enabled=true`.

**AR-51. One sandbox per visitor**, seeded through modules' public commands (never SQL inserts); login by persona choice, no email.

**AR-52. RLS isolates sandboxes.** `db/migration-demo` adds `sandbox_id` (default from `current_setting('verni.sandbox_id')`) and an RLS policy to **every business table**; the backend sets `SET LOCAL verni.sandbox_id` per transaction; the app role has no `BYPASSRLS`; a test proves A cannot read B. Business code and the production schema know nothing about sandboxes.

**AR-53. No external side effects:** `DemoMailbox` instead of SMTP.

**AR-54. Limits:** 24 h TTL with cleanup (separate RLS-bypassing role), per-IP and global caps (`429`), "Демо" badge on every screen.

**AR-55. Race demo:** "Two claimants at once" sends two real parallel `agree-handover` calls and shows the `409`.

---

## 10. Testing

**AR-56. Pyramid and tools.**
- `domain` is covered by pure unit tests (JUnit 5, no Spring).
- `application` + `persistence` are covered by integration tests with Spring Boot Test + Testcontainers.
- End-to-end scenarios use Playwright.
- Unit tests run with Surefire (`*Test`); tests that need Docker run with Failsafe (`*IT`).

**AR-57. Only real PostgreSQL + PostGIS** (Testcontainers, the same major version as production). H2 and embedded substitutes are forbidden. The container is reused across test classes.

**AR-58. Every endpoint is covered** by a positive test and negative access tests (AR-32).

**AR-59. Leak test:** for every public endpoint, verify that the JSON response **does not contain** forbidden fields (email, hidden features, exact point, private participant IDs). This is the automated guard for E2E-04.

**AR-60. Race tests are mandatory** for the invariants AR-14–AR-16. Pattern: two parallel transactions (`CountDownLatch`/`ExecutorService`) against a real DB. Expected result: exactly one success and one `409`, with no extra audit/outbox records.

**AR-61. Mandatory E2E: E2E-01…E2E-06** from the business requirements. The Pilot requires green E2E-01, 02, 04, 05.

**AR-62. Test data** is created with builders/factories, not shared SQL dumps. Tests do not depend on each other or on execution order.

---

## 11. Code, configuration, observability

**AR-63. Java style.**
- DTOs, commands, events and value objects are `record`s.
- No Lombok **[default]**.
- Null safety: JSpecify `@NullMarked` in `package-info.java`.
- `Optional` only as a return value.
- Formatting: Spotless (google-java-format), checked in `mvn verify`.

**AR-64. Naming.**
- Commands use the imperative: `AgreeHandover`.
- Events use the past tense: `HandoverAgreed`.
- Use cases: `<Verb><Entity>UseCase` or a method in `<Module>Commands`.
- Error codes: `UPPER_SNAKE_CASE`.

**AR-65. Configuration** is typed via `@ConfigurationProperties` with validation; scattered `@Value` is forbidden. Profiles: `local`, `test`, `staging`, `production`, `demo`. Environment differences are expressed only through configuration, never with `if (prod)` in code.

**AR-66. Observability.**
- Structured JSON logs (Spring Boot built-in structured logging).
- `traceId` in every log line and in Problem Details.
- Actuator health/readiness.
- Micrometer business metrics: `verni_listings_published_total`, `verni_handover_completed_total`, `verni_outbox_dead_jobs`.
- Product analytics (business requirements §9) is a separate anonymized stream, not logs.

---

## 12. Process

**AR-67. Monorepo** with `backend/`, `frontend/`, `infrastructure/`, `docs/` (as in the stack).
- Short-lived branches, pull requests into `main`; `main` is always deployable.
- Commit messages follow Conventional Commits (`feat(responses): …`).
- The language of code, comments, commits and documentation is English; the product UI is in Russian.

**AR-68. ADRs** ([index](adr/README.md), [template](adr/0000-template.md)) are required for: an open product/technical decision, a new arrow in AR-03, a new technology, a deviation from a rule.

**AR-69. Definition of Done for a task:** code + tests per §10, updated OpenAPI spec, migration (if needed), a learning note in `docs/learning/`, green CI, a PR that answers the checklist in [`.github/pull_request_template.md`](../.github/pull_request_template.md). Process: [workflow.md](workflow.md).
