# VERNI — Tech Stack and Architecture

> **Version:** 1.2 · **Date:** 2026-10-11 · Rules: [architecture-rules.md](architecture-rules.md) · Decisions: [adr/](adr/README.md)

## 1. Technical goal

A phone-friendly platform for lost and found items: public feed and map, fast publishing, private responses, match suggestions, personal Kanban. A **modular monolith** with a REST API; no microservices, Kubernetes, Kafka or AI dependencies in the MVP.

## 2. Technology stack

| Layer | Technology |
|---|---|
| Backend | **Java 25 LTS**, **Spring Boot 4.1** (Web MVC, Security, Data JPA, `JdbcClient` for PostGIS SQL), Maven Wrapper |
| Modules | Spring Modulith 2.1 (boundary verification) |
| Auth | Spring Security, server-side sessions (Spring Session JDBC), HttpOnly cookies |
| Database | PostgreSQL + PostGIS, Flyway |
| Background jobs | PostgreSQL outbox + Spring scheduler |
| API | REST `/api/v1`, OpenAPI 3 via springdoc (code-first) |
| Frontend | Next.js 16 + React + TypeScript, Tailwind CSS + shadcn/ui, MapLibre GL JS + licensed tiles |
| Photos | S3-compatible object storage |
| Tests | JUnit 5, Spring Boot Test, Testcontainers; Vitest, React Testing Library, Playwright |
| Observability | Actuator, Micrometer, structured JSON logs |
| Infra / CI | Docker Compose, reverse proxy with TLS, GitHub Actions, GHCR; VPS in Russia (Selectel / Cloud.ru candidates) |

Exact versions are pinned in `backend/pom.xml` and `frontend/package-lock.json`.

## 3. System overview

```text
Browser → HTTPS reverse proxy → Next.js (/)  ──┐
                              → Spring Boot (/api/*) modular monolith
                                   ├─ PostgreSQL/PostGIS (+ outbox jobs worker)
                                   ├─ S3 object storage
                                   └─ Email provider
```

Frontend and API share **one domain** (simpler cookies, CSRF, no CORS).

| Module | Responsibility |
|---|---|
| `identity` | Email login, profiles, sessions, blocking |
| `listings` | Item cards, categories, photos, publication status |
| `discovery` | Feed, map, filters, full-text search, match candidates |
| `responses` | Responses, handover agreement, return confirmation, invariants |
| `messaging` | Private messages within a response |
| `media` | Image intake, processing, storage |
| `moderation` | Reports, hiding, moderator decisions |
| `notifications` | Email via the outbox |
| `audit` | History of significant actions |
| `demo` | Demo sandboxes (demo deployment only) |
| `shared` | Technical building blocks only |

Allowed dependencies between modules: AR-03.

## 4. Key data

| Table | Main fields |
|---|---|
| `users` | id, email, display_name, status, timestamps |
| sessions / auth tokens | expiry, revocation, hashed one-time tokens |
| `listings` | id, author_id, type (`LOST`/`FOUND`), title, description, category_id, happened_at, location, public_location, public_features, hidden_features (author only), status, closed_reason |
| `listing_photos` | listing_id, storage_key, order, metadata |
| `categories` | dictionary, `sensitive` flag |
| `responses` | listing_id, responder_id, status, agreed_at, author_confirmed_at, responder_confirmed_at, completed_at |
| `messages` | response_id, sender_id, body, created_at |
| `matches` (optional) | listing_id, candidate_id, score, algorithm version |
| `reports` | reporter_id, listing_id, reason, moderation_status |
| `listing_events` | actor_id, listing_id, action, timestamp |
| `outbox_jobs` | type, payload, attempts, next_run_at, state, idempotency_key |

The detailed ERD grows with the migrations. Denormalize only based on measurements.

## 5. Geolocation and matching

Rules: AR-24, ADR-0003. Matching = opposite type + category + public-point distance + time window + public features; results are candidates, never proof of ownership. No ML in the MVP. GPS only on a direct user action, with a manual alternative.

## 6. Authorization, access and security

Rules: AR-31…AR-38. Additionally:

1. Guests browse the map, feed and public cards.
2. A draft is prepared before login (ADR-0004); publishing requires a confirmed email.
3. Authors change only their listings; participants see only their conversations; moderators have limited admin access.

**§6.8 Personal data:** account deletion, blocking and retention follow Russian personal data law; a policy and legal review are required before launch (#16).

## 7. Consistency rules

AR-14…AR-18.

## 8. API (contract sketch)

```http
POST   /api/v1/auth/email/request | /auth/email/verify | /auth/logout
GET    /api/v1/me

GET    /api/v1/listings?type=&category=&lat=&lon=&radius_km=&q=&cursor=
POST   /api/v1/listings
GET    /api/v1/listings/{id}
PATCH  /api/v1/listings/{id}
POST   /api/v1/listings/{id}/close
GET    /api/v1/listings/{id}/matches

POST   /api/v1/listings/{id}/responses
GET    /api/v1/me/responses
POST   /api/v1/responses/{id}/messages
POST   /api/v1/responses/{id}/agree-handover | cancel-handover | confirm-return | reject

GET    /api/v1/me/board
GET    /api/v1/media/{id}
POST   /api/v1/reports
GET    /api/v1/admin/reports
POST   /api/v1/admin/reports/{id}/resolve
POST   /api/v1/demo/sandboxes          (demo deployment only)
```

Contract rules: AR-26…AR-30.

## 9. Background processes

Email codes, notifications about responses/messages, handover and confirmation reminders, cleanup of expired tokens, temporary files and demo sandboxes. Rules: AR-47…AR-49.

## 10. Frontend and UX

Rules: AR-39…AR-46; screens and tokens: [design/frontend-spec.md](design/frontend-spec.md). Mobile-first from 360 px, WCAG 2.2 AA target, readable URLs with SEO/OG for public cards, PWA (installable shell, **no offline promises**), the feed always works without the map.

## 11. CI/CD and operations

Pipeline and environments: [workflow.md](workflow.md) §5–§6. Production baseline: HTTPS reverse proxy; separate frontend, backend and PostgreSQL services; object storage with backups and lifecycle policy; PostgreSQL backups to independent storage with tested restore; health/readiness, JSON logs, error tracking, metrics and alerts; secrets outside Git; restricted DB/admin access; environments `local`, `staging`, `demo`, `production`.

## 12. Verifiable technical outcomes

| ID | Done criterion |
|---|---|
| TECH-01 | A new environment starts locally with documented commands; CI passes without manual steps |
| TECH-02 | The API is documented with OpenAPI; frontend contracts are typed |
| TECH-03 | Geo search is correct in tests with real PostGIS and a spatial index |
| TECH-04 | Access rights are covered by negative integration tests |
| TECH-05 | Concurrent requests never create two agreed handovers |
| TECH-06 | Published photos contain no original EXIF coordinates |
| TECH-07 | Notifications survive a short SMTP outage and are retried without duplicates |
| TECH-08 | E2E passes: "lost → found → contacted → returned" and the two-claimant conflict |
| TECH-09 | DB restore from backup is tested and documented |
| TECH-10 | The public demo cannot change real user data |

Targets to set on staging (not current facts): search p95 budget (e.g. < 500 ms on a fixed dataset), mobile Web Vitals, share of completed user scenarios.

## 13. Deliberately not in the MVP

Microservices, Kubernetes, Kafka/RabbitMQ, WebFlux, GraphQL, native apps, AI image search, SMS/Gosuslugi/VK ID, payments, ratings, organizations, automatic ownership verification, WebSocket chat, ML recommendations.

## 14. Open technical decisions

- Login method: email OTP or magic link (#7).
- Mandatory photo for "found" posts and photo limits (#9).
- Email provider; licensed tile provider for Russia (#5, #10).
- Category set and match ranking criteria (#18).
- Dispute resolution for handovers (#13).

Accepted decisions: [adr/README.md](adr/README.md).
