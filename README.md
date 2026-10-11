# VERNI

A web platform that helps people find and return lost items: a public feed and map, fast posting, private responses, verified handovers.

## Documentation

- [Business requirements](docs/business-requirements.md)
- [Tech stack and architecture](docs/tech-stack.md)
- [Architecture rules](docs/architecture-rules.md) — mandatory for every change
- [Architecture decision records](docs/adr/)

## Repository layout

```text
backend/          Spring Boot 4.1 modular monolith (Java 25, Maven)
frontend/         Next.js 16 + React + TypeScript + Tailwind CSS
infrastructure/   Dockerfiles and reverse proxy configuration
docs/             Requirements, rules, ADRs, OpenAPI spec
compose.yaml      Local development dependencies
```

## Local development

Requirements: JDK 25, Node.js 24, Docker.

```bash
docker compose up -d                                  # PostgreSQL/PostGIS + Mailpit (http://localhost:8025)

cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
./mvnw verify                                         # format check, unit and integration tests (needs Docker)
./mvnw spotless:apply                                 # format code

cd frontend
npm ci
npm run dev                                           # http://localhost:3000
npm run lint && npm run typecheck && npm test
```

Without a local database the backend can also start against Testcontainers: run `TestVerniApplication` from `backend/src/test`.
