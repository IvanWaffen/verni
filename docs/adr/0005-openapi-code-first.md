# 0005. OpenAPI: code first, then the spec

- **Status:** accepted
- **Date:** 2026-10-11
- **Affects:** backend `web` layer, frontend API client, CI; AR-27, AR-40; TECH-02

## Context

The frontend must work against a typed contract. Writing the spec by hand and then keeping it in sync with the code is extra overhead in a single-author project.

## Decision

- Controllers and DTOs are written first; springdoc generates the spec.
- The generated file is committed to `docs/api/openapi.yaml`.
- CI checks that the file is up to date and runs oasdiff against `main`. A breaking change without a new API version fails the build.
- The frontend generates types from the committed file (openapi-typescript); hand-written copies of types are forbidden.

## Alternatives

- **Design-first (YAML first, then code).** Better for several teams, but double work here.

## Consequences

- The contract is discussed in PRs via the `openapi.yaml` diff.
- springdoc annotations (descriptions, examples, error codes) are part of the Definition of Done for an endpoint.
