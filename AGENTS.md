# AGENTS.md

Instructions for AI coding agents (Claude Code and others) working in this repository.

## Project

VERNI is a web platform for returning lost items (Russia, Russian UI). It is a Spring Boot 4.1 / Java 25 modular monolith (`backend/`) plus a Next.js 16 frontend (`frontend/`), PostgreSQL + PostGIS, deployed with Docker Compose.

## What to read (keep context small)

**Do not read whole documents.** Start from the issue: it cites the relevant `BR-xx`, `AR-xx`, ADRs and spec sections. Read only those sections (`grep -n "AR-24" docs/architecture-rules.md`, then read that range).

| Task touches | Read |
|---|---|
| Any backend code | `docs/architecture-rules.md` §1–§3 (boundaries, layers, transactions) once per session, plus the cited rules |
| Data / migrations | architecture-rules §4 |
| REST endpoints | architecture-rules §5–§6 |
| UI | `docs/design/frontend-spec.md` §1–§3 + the screen section cited in the issue; mockup image only if layout is unclear; architecture-rules §7 |
| Background jobs / email | architecture-rules §8 |
| Demo / new business table | architecture-rules §9 |
| Tests | architecture-rules §10 |
| Business behavior unclear | the cited `BR-xx` / lifecycle in `docs/business-requirements.md` §4–§6; ask the owner if still unclear |
| Decisions | `docs/adr/README.md` index, then the specific ADR |
| Next.js APIs | [frontend/AGENTS.md](frontend/AGENTS.md): the bundled docs in `node_modules/next/dist/docs/` |

`docs/tech-stack.md` (stack overview, API sketch) and `docs/workflow.md` (human-oriented process) are reference material; this file already summarizes what an agent needs from them.

## The owner

The owner is a PHP/Laravel developer learning Java/Spring through this project. Explain Java/Spring concepts with Laravel analogies. Ask the owner about business questions; do not guess.

## Workflow (summary of docs/workflow.md)

1. **Work only from a GitHub issue.** Read it with `gh issue view <n>`.
2. **Large issues:** post a plan as an issue comment (approach, modules, migrations, risks, ADRs, proposed PR split) and wait for the owner's approval. Small issues: start right away.
3. **Open decisions** (`needs-decision`): ask the owner, then record an ADR in `docs/adr/` using the [template](docs/adr/0000-template.md).
4. **Branch:** `<type>/<issue>-<slug>` from an up-to-date `main`, e.g. `feat/12-listing-create`. Never commit to `main` directly.
5. **Commits:** Conventional Commits with the module as scope: `feat(listings): ...`. End commit messages with the `Co-Authored-By` trailer.
6. **Learning note (mandatory for code changes):** add `docs/learning/NNNN-<slug>.md` following [docs/learning/README.md](docs/learning/README.md) and update [docs/learning/laravel-to-spring.md](docs/learning/laravel-to-spring.md) with new concept pairs.
7. **Verify locally** before pushing (see Commands).
8. **PR:** `gh pr create` with a Conventional Commits title. Allowed scopes (`pr-title` check): `identity listings discovery responses messaging media moderation notifications audit demo shared backend frontend infra ci docs deps`, or no scope. The template asks for issue link, verification steps, learning note and the AR checklist. Then watch CI (`gh pr checks --watch`) and fix failures.
9. **Never merge PRs.** The owner reviews and squash-merges.
10. **Board:** move the issue on the [project board](https://github.com/users/IvanWaffen/projects/1) to `In progress` when work starts and to `Review` when the PR is open (merged/closed items move to `Done` automatically). Add new issues and PRs to the board.

```bash
# project id PVT_kwHOEHBS7c4BmlA3, Status field PVTSSF_lAHOEHBS7c4BmlA3zhlNccE
# options: Backlog b38aff69, Ready 2af98023, In progress 88dbd9f4, Review 6c9722ad, Done 1189d492
ITEM=$(gh project item-add 1 --owner IvanWaffen --url <issue-or-pr-url> --format json -q .id)
gh project item-edit --project-id PVT_kwHOEHBS7c4BmlA3 --id "$ITEM" \
  --field-id PVTSSF_lAHOEHBS7c4BmlA3zhlNccE --single-select-option-id 88dbd9f4
```

## Known gh CLI issues

`gh pr edit` (and other commands that query PR `projectCards`) fails with "Projects (classic) is being deprecated". Use the REST API instead:

```bash
gh api -X PATCH repos/IvanWaffen/verni/pulls/<n> -f title="..."          # also: -f body="..." , -f base=main
gh api -X PUT repos/IvanWaffen/verni/pulls/<n>/update-branch             # PR is BEHIND main
gh api repos/IvanWaffen/verni/issues/<n>/labels -f "labels[]=blocked"    # add a label
```

## Commands

```bash
docker compose up -d                                    # local PostgreSQL/PostGIS + Mailpit

cd backend
./mvnw spotless:apply                                   # format Java
./mvnw verify                                           # format check + unit + integration tests (needs Docker)
./mvnw test                                             # unit tests only (no Docker)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

cd frontend
npm run format && npm run lint && npm run typecheck && npm test && npm run build
```

If JDK 25 or Docker is missing locally, say so and ask the owner rather than skipping checks silently. CI runs the full suite in any case.

## Hard rules (most often violated)

- Module boundaries: other modules use only a module's root package; no imports from `internal`/`web` (AR-02–AR-04). `ModularityTest` enforces the dependency graph in each module's `package-info.java`.
- Status changes only through aggregate methods (AR-10); the server computes `allowedActions` (AR-11).
- One use case = one `@Transactional`; no network calls inside; side effects via the outbox (AR-12, AR-13, AR-47).
- Separate DTOs for public, owner and moderator views; never leak email, hidden features, exact location (AR-29, AR-36).
- Flyway migrations only, never edit an applied one, keep them backward compatible (AR-19).
- Every new business table needs an RLS policy in `db/migration-demo` (AR-52).
- Real PostgreSQL/PostGIS in tests (Testcontainers); no H2 (AR-57).
- Code, comments, commits, docs: English. Product UI texts: Russian.
