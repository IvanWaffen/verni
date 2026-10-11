# VERNI — Tech Stack and Architecture

> **Version:** 1.1 (solution design)  
> **Date:** 2026-10-11  
> **Status:** agreed baseline stack for MVP planning  
> **Related documents:** [business-requirements.md](business-requirements.md), [architecture-rules.md](architecture-rules.md)

## 1. Technical goal

Build a phone-friendly platform for finding lost and found items with a public feed and map, fast publishing, protected responses, match suggestions and a personal Kanban board.

**Priorities:** a complete user scenario → security → mobile usability → architecture and test quality → performance based on real metrics.

Strategy: a **modular monolith** with a REST API; no microservices, Kubernetes, Kafka or mandatory AI dependencies in the MVP.

## 2. Technology stack

| Layer | Chosen technology | Why |
|---|---|---|
| Backend language | **Java 25 LTS** | Long-term support; records, modern JVM stack |
| Backend framework | **Spring Boot 4.1.1** | Web API, validation, security, transactions, observability |
| HTTP | Spring Web MVC | Predictable request/response model; WebFlux is not needed |
| Authentication | Spring Security + server-side sessions (Spring Session JDBC), HttpOnly cookies | Secure login via one-time email code/link |
| Business logic and data | Spring Data JPA / Hibernate | Typical CRUD and transactional domain operations |
| Geo search | Spring JDBC (`JdbcClient`) + PostGIS SQL | Controlled spatial queries |
| Database | PostgreSQL + PostGIS | Reliable SQL model, indexes and distance search |
| Migrations | Flyway | Schema versioning, migrations in CI |
| Build | Maven + Maven Wrapper | Reproducible builds and a single lifecycle |
| Frontend | Next.js + React + TypeScript | Public SEO pages, mobile UI, PWA |
| UI | Tailwind CSS + shadcn/ui | Fast design system and form development |
| Map | MapLibre GL JS + a licensed tile provider | Map vendor flexibility, geo UI support |
| Photos | S3-compatible Object Storage | Images stored separately from the app |
| Background jobs | PostgreSQL job/outbox + Spring scheduler | Durable notification jobs and retries |
| API documentation | OpenAPI 3 (springdoc, code-first) | Frontend/backend contract; spec generation ([ADR-0005](adr/0005-openapi-code-first.md)) |
| Backend testing | JUnit 5 + Spring Boot Test + Testcontainers | Domain, integration and transactional tests |
| Frontend testing | Vitest + React Testing Library + Playwright | UI components and end-to-end scenarios |
| Architecture checks | Spring Modulith 2.1 | Module boundary verification |
| Observability | Spring Boot Actuator + Micrometer + structured logs | Health, metrics, diagnostics |
| Infrastructure | Docker Compose + reverse proxy + TLS | Simple reproducible production deployment |
| CI/CD | GitHub Actions | Build, lint, test, security checks, deploy |
| Hosting | VPS/cloud VM in Russia; Selectel or Cloud.ru as candidates | Availability for the Russian audience |

**Version notes:** Java 25 is the chosen LTS line; Spring Boot 4.1.1 is the stable version as of the document date. Exact patch versions are pinned in `pom.xml` / the lock file at initialization and then updated with compatibility checks.

Version sources: https://www.oracle.com/java/technologies/java-se-support-roadmap.html and https://spring.io/projects/spring-boot.

## 3. System overview

```text
User (mobile/desktop browser)
                    |
          HTTPS / reverse proxy
                    |
             Next.js frontend
                    |
             /api/v1 REST
                    |
        Spring Boot modular monolith
  +-------------+-----------+--------------+
  | Identity    | Listings  | Discovery    |
  | Responses   | Messaging | Moderation   |
  | Media       | Notifier  | Audit        |
  +-------------+-----------+--------------+
        |                  |           |
   PostgreSQL/PostGIS      S3     Email provider
        |
   Outbox / jobs worker
```

Frontend and API should preferably live behind **one public domain** (`/` and `/api/*`) to simplify cookies, CSRF and CORS.

### Modules

- `identity` — email authentication, profiles, sessions, blocking.
- `listings` — item cards, categories, photos, publication status.
- `discovery` — map, filters, full-text search, match candidates.
- `responses` — responses, handover agreement, return completion, invariants.
- `messaging` — private messages within a response.
- `media` — image intake, processing and storage.
- `moderation` — reports, hiding posts, moderator decisions.
- `notifications` — email events, templates, retries.
- `audit` — history of significant actions and state transitions.
- `demo` — demo sandboxes; active only in the demo deployment ([ADR-0001](adr/0001-demo-server-sandbox.md)).

Externally, modules interact through public service interfaces/application events, **never accessing a neighbor module's internal repositories directly**. A separate database per module is not required for the first release.

### Directory structure

```text
verni/
├── backend/
│   ├── src/main/java/ru/verni/
│   │   ├── identity/
│   │   ├── listings/
│   │   ├── discovery/
│   │   ├── responses/
│   │   ├── messaging/
│   │   ├── media/
│   │   ├── moderation/
│   │   ├── notifications/
│   │   ├── audit/
│   │   ├── demo/
│   │   └── shared/
│   ├── src/main/resources/db/migration/
│   ├── src/main/resources/db/migration-demo/
│   └── pom.xml
├── frontend/
├── infrastructure/
│   ├── docker/
│   └── proxy/
├── docs/
│   ├── adr/
│   └── api/
├── .github/workflows/
└── compose.yaml
```

## 4. Key data

Proposed aggregates and tables (a detailed ERD is a separate artifact after the business rules are agreed):

| Entity | Main fields / purpose |
|---|---|
| `users` | id, email, display_name, status, timestamps |
| `sessions` / auth tokens | expiry, revocation, one-time email tokens (hashed) |
| `listings` | id, author_id, type (`LOST`/`FOUND`), title, description, category_id, happened_at, location, public_location, public_features, hidden_features (author only), status, closed_reason |
| `listing_photos` | listing_id, storage_key, order, image metadata |
| `categories` | item category dictionary, `sensitive` flag |
| `responses` | listing_id, responder_id, status, agreed_at, author_confirmed_at, responder_confirmed_at, completed_at |
| `messages` | response_id, sender_id, body, created_at |
| `matches` (optional) | listing_id, candidate_id, score, algorithm/version, created_at |
| `reports` | reporter_id, listing_id, reason, moderation_status |
| `listing_events` | actor_id, listing_id, transition/action, timestamp |
| `outbox_jobs` | type, payload, attempts, next_run_at, state, idempotency_key |

- Public entity IDs are UUIDv7 generated by the application ([ADR-0006](adr/0006-uuidv7-identifiers.md)).
- Introduce denormalization only based on measurements.
- Store the actual date of loss/find separately from the publication date.
- The public API **never returns email, hidden features, IP or exact personal coordinates**.

## 5. Geolocation and matching

- For a post the user specifies an **approximate area** on the map; an exact home address is not needed.
- The public point is snapped to a grid: 500 m by default, 1500 m for sensitive categories ([ADR-0003](adr/0003-public-location-coarsening.md)).
- Use PostGIS `geography(Point, 4326)` with a `GiST` index.
- Radius search uses `ST_DWithin` over the public point, with pagination and a bounded result area; filters by type, category, date and status.
- Matching: opposite post type + category + proximity in time/space + optional public features. These are **candidates**, not proof of ownership.
- Never reveal hidden distinguishing features specified for later owner verification.
- No ML, photo recognition or paid AI APIs at the start.
- Use geodata only on a direct user action; GPS access requires browser permission and a manual alternative.

## 6. Authorization, access and security

1. Guests can view the map, feed and public cards without logging in.
2. A user can prepare a draft before logging in (stored on the client, [ADR-0004](adr/0004-client-side-draft.md)); before publishing they confirm their email with a one-time code or link.
3. The backend uses a server-side session with `Secure`, `HttpOnly`, `SameSite` cookies; CSRF protection for state-changing requests.
4. An author edits and closes only their own post; a responder sees only their own conversations; a moderator has limited administrative access.
5. Rate limits on login, posting, responses and messages; anti-spam and reports are mandatory.
6. Photos: allowed formats and limits, server-side re-encoding, EXIF stripping, MIME/signature checks, random storage keys, access control on upload.
7. Never show hidden contacts or exact addresses in public cards or SEO meta tags.
8. Account deletion/blocking and data retention are designed with applicable Russian personal data law in mind. A data processing policy and a legal review are required before launch.
9. All tokens, SMTP/S3 credentials and DB passwords come from environment variables/CI secrets; never commit them to the repository.
10. Logs contain no tokens, messages or sensitive user information.

## 7. Consistency rules

- Posts and responses have **separate state machines**.
- A card can have several responses, but only one active agreed handover at a time.
- Agreeing a handover is a transactional operation with authorization and state checks. A repeated command is idempotent or ends with an expected conflict without side effects.
- Completing a return (second confirmation) closes the post; new responses are forbidden, remaining open responses move to a proper terminal state.
- For concurrent operations: unique constraints and/or row locks, race tests.
- Audit events and notification jobs are stored in the same transaction as the business state change (transactional outbox).

## 8. API (contract sketch)

```http
POST   /api/v1/auth/email/request
POST   /api/v1/auth/email/verify
POST   /api/v1/auth/logout
GET    /api/v1/me

GET    /api/v1/listings?type=LOST&lat=...&lon=...&radius_km=...
POST   /api/v1/listings
GET    /api/v1/listings/{id}
PATCH  /api/v1/listings/{id}
POST   /api/v1/listings/{id}/close
GET    /api/v1/listings/{id}/matches

POST   /api/v1/listings/{id}/responses
GET    /api/v1/me/responses
POST   /api/v1/responses/{id}/messages
POST   /api/v1/responses/{id}/agree-handover
POST   /api/v1/responses/{id}/cancel-handover
POST   /api/v1/responses/{id}/confirm-return
POST   /api/v1/responses/{id}/reject

GET    /api/v1/me/board
POST   /api/v1/reports
GET    /api/v1/admin/reports
POST   /api/v1/admin/reports/{id}/resolve

POST   /api/v1/demo/sandboxes            (demo deployment only)
```

- The contract is code-first: the OpenAPI spec is generated from the code and committed ([ADR-0005](adr/0005-openapi-code-first.md)).
- Errors use a single Problem Details format (`application/problem+json`), validated input DTOs and stable error codes.
- Lists use cursor pagination.
- Public GET endpoints are available without a session; state-changing operations require authentication and authorization.

## 9. Background processes

MVP: sending email codes, notifications about new responses/messages, reminders about agreed handovers and pending return confirmations, periodic cleanup of expired tokens, temporary files and expired demo sandboxes.

Implementation: an `outbox_jobs` table, claimed with `FOR UPDATE SKIP LOCKED`, attempt limit, backoff, error monitoring, idempotent handlers. A separate message broker is not needed yet.

## 10. Frontend and UX

- Mobile-first, target widths from **360 px**; tablet/desktop as well.
- Pages: home feed/map, publishing, item card, responses and conversations, personal Kanban board, profile, help, policies and rules, moderation.
- Minimum steps in the publishing flow: photo → "lost/found" → location → short description → publish (login without losing the draft).
- A public card has a readable URL, SEO metadata, OG preview and a "Share" button.
- Accessibility: keyboard navigation, labeled form fields, clear errors, contrast; target WCAG 2.2 AA.
- PWA: install to the home screen and shell caching; **do not promise full offline mode**.
- The map is an additional way to search; the full feed remains available without the map.

## 11. CI/CD and operations

### Pipeline (GitHub Actions)

1. Format / style / static checks (`mvn verify`, frontend lint/typecheck).
2. Unit tests.
3. Integration tests with PostgreSQL/PostGIS via Testcontainers.
4. Building backend and frontend containers.
5. Scanning dependencies/images for known vulnerabilities.
6. Deploying to staging; Playwright smoke/E2E.
7. Manual approval of the production deployment; post-release health check.

### Production baseline

- HTTPS, reverse proxy, separate frontend / backend / PostgreSQL services.
- Object Storage with backups and an object lifecycle policy.
- Regular PostgreSQL backups **to independent storage**; restore testing.
- Health/readiness endpoints, structured JSON logs, error tracking, basic technical metrics and alerts.
- Secrets outside Git; restricted access to the DB and the admin panel.
- Separate `local`, `staging`, `production` and `demo` environments.

## 12. Verifiable technical outcomes

| ID | Done criterion |
|---|---|
| TECH-01 | A new environment starts locally with documented commands; CI passes without manual steps |
| TECH-02 | The API is documented with OpenAPI; contracts are typed on the frontend |
| TECH-03 | Geo search is correct in tests with real PostGIS and a spatial index |
| TECH-04 | Access rights are covered by negative integration tests |
| TECH-05 | Concurrent requests do not create two simultaneously agreed handovers |
| TECH-06 | Photos contain no original EXIF coordinates after publishing |
| TECH-07 | Notifications survive a short SMTP outage and are retried without mass duplicates |
| TECH-08 | E2E passes: "lost → found → contacted → returned" and the two-claimant conflict |
| TECH-09 | Restoring the DB from a backup is tested and documented |
| TECH-10 | The public demo mode cannot change real user data |

**Targets, not current facts:** on a test environment define a search response time budget (e.g. p95 < 500 ms for a fixed dataset and load profile), measure mobile Web Vitals and the share of successfully completed user scenarios.

## 13. Deliberately not in the MVP

Microservices; Kubernetes; Kafka/RabbitMQ; WebFlux; GraphQL; native mobile apps; AI image search; SMS/Gosuslugi/VK ID; payments; ratings; organizations with role hierarchies; automatic ownership verification; full real-time WebSocket chat; complex ML recommendations.

## 14. Open decisions before development

- Passwordless login method: magic link or email OTP (choose one primary UX) — **open**.
- Whether a photo is mandatory for "found" posts and allowed exceptions — **open**.
- ~~Rules for approximate location and the public radius.~~ Decided: [ADR-0003](adr/0003-public-location-coarsening.md).
- Email provider and a licensed tile source for Russia; terms and budget — **open**.
- Exact category set and ranking criteria for match candidates — **open**.
- ~~Return confirmation by one or both sides.~~ Decided: [ADR-0002](adr/0002-two-party-return-confirmation.md). Dispute resolution remains **open**.

These items are recorded as separate ADRs before the corresponding modules are implemented. Other accepted decisions: [ADR-0001](adr/0001-demo-server-sandbox.md) (demo server sandbox), [ADR-0004](adr/0004-client-side-draft.md) (client-side draft), [ADR-0005](adr/0005-openapi-code-first.md) (code-first OpenAPI), [ADR-0006](adr/0006-uuidv7-identifiers.md) (UUIDv7). Development rules are in [architecture-rules.md](architecture-rules.md).
