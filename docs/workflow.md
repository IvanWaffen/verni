# VERNI — Development Workflow

> **Date:** 2026-10-11  
> **Related:** [architecture-rules.md](architecture-rules.md), [learning/](learning/)

## 1. Task process

**Tracker:** GitHub Issues (+ a GitHub Projects board).

- Milestones: `Pilot`, `Release 1.0`.
- Labels: `type:*` (feature, infra, bug, chore), `module:*`, `area:backend` / `area:frontend`, `P0` / `P1`, `blocked`, `needs-decision`.
- Large issues ("epics") describe a business capability. Before work starts they may be split into smaller sub-issues, each a working **vertical slice** (migration → domain → API → UI → tests).

**Lifecycle of a task:**

1. The owner picks an issue ("take #12").
2. **Plan.** For large issues the agent posts a plan as an issue comment: approach, modules, migrations, risks, ADRs needed, proposed split into PRs. Work starts after the owner approves. Small issues start right away.
3. **Open decisions** (`needs-decision` label) are resolved with the owner first and recorded as ADRs.
4. Branch → implementation → tests → learning note → PR.
5. CI is green → the owner reviews and merges (squash). The agent never merges.
6. `Closes #12` closes the issue on merge.

## 2. Branches

Trunk-based development. `main` is always deployable; there is no `develop`.

- One issue (or sub-issue) = one branch = one PR; a branch lives 1–3 days.
- Naming: `<type>/<issue>-<slug>`, e.g. `feat/12-listing-create`, `fix/31-feed-pagination`, `chore/40-bump-spring`, `docs/7-adr-login`.
- A feature that does not fit one PR is split into sequential PRs; unfinished UI is hidden behind a configuration flag.
- Rebase on `main` before merging (the ruleset requires an up-to-date branch).

## 3. Commits

[Conventional Commits](https://www.conventionalcommits.org/) with the module as the scope:

```text
feat(listings): create listing with approximate location
fix(responses): return 409 when handover is already agreed
test(responses): add race test for concurrent agree-handover
docs(adr): 0007 choose email OTP for login
```

- Types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`.
- Commits inside a branch are atomic and meaningful.
- **Squash merge only.** `main` gets one commit per PR, titled with the PR title. CI checks the PR title format (`pr-title` job).

## 4. `main` protection (GitHub ruleset)

- No direct pushes; changes only via pull requests.
- Required checks: `backend`, `frontend`, `images`, `secrets`, `pr-title`; the branch must be up to date.
- Squash merge only, linear history, all review threads resolved.
- No force pushes, no branch deletion.
- Approval is **not required** (one developer; PRs are opened from the owner's account). Merging by the owner is the review.

## 5. CI/CD

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | PR, push to `main` | Backend `mvn verify` (Spotless, unit + Testcontainers tests); frontend format/lint/typecheck/test/build; Docker image builds; gitleaks secret scan; dependency review (PR only) |
| `pr-title.yml` | PR | Conventional Commits check of the PR title |
| `publish.yml` | CI success on `main` | Builds and pushes images to GHCR, tagged with the commit SHA and `main` |
| Dependabot | Weekly | Grouped updates for Maven, npm, GitHub Actions, Docker base images |

Third-party actions are pinned by commit SHA; official `actions/*` by major tag.

**Planned (when a VPS and domain exist):**

- `deploy-staging.yml`: after `publish`, SSH deploy of the SHA-tagged images to staging (`docker compose pull && up -d`), then a smoke test (`/actuator/health`, home page).
- `deploy-production.yml`: manual run with the `production` GitHub Environment approval; promotes **the same image SHA** that passed staging. Rollback = deploy the previous SHA.
- Image vulnerability scanning and Playwright E2E against the compose stack.

## 6. Environments and deployment

| Environment | Where | How |
|---|---|---|
| `local` | Developer machine | `docker compose up -d` + `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` + `npm run dev` |
| `staging` | VPS in Russia (TBD) | Docker Compose + Caddy (automatic TLS), deployed from GitHub Actions over SSH by a non-root deploy user |
| `demo` | Same VPS as staging at first | Separate deployment, DB and bucket ([ADR-0001](adr/0001-demo-server-sandbox.md)) |
| `production` | Separate VPS (before the Pilot) | Manual promotion of a staging-verified image |

- Secrets live in GitHub Environments and are written to `.env` on the server during deployment. Never commit them.
- Flyway runs on backend startup. **Migrations must be backward compatible** (expand → migrate → contract across releases), so that rolling back to the previous image works.
- No self-hosted runners: the repository is public, and a fork PR could run code on the server.

## 7. Learning notes

The owner is moving from PHP/Laravel to Java/Spring. **Every PR with code changes includes a learning note** in [`docs/learning/`](learning/), following the [format](learning/README.md):

- what was done;
- new Java/Spring concepts, each with **its Laravel analogue**;
- where to look in the code (`file:line`);
- pitfalls compared to PHP;
- 1–3 links to official documentation;
- an optional exercise.

New concept pairs are also added to the cumulative [Laravel → Spring glossary](learning/laravel-to-spring.md). PRs without code (pure docs/config) may skip the note.

## 8. Definition of Done

- Code + tests per [architecture-rules.md §10](architecture-rules.md#10-testing).
- OpenAPI spec regenerated (once the API exists).
- Migration, if needed, backward compatible.
- Learning note written.
- CI green; the PR template checklist is answered.
- ADR added if a decision or rule changed.
