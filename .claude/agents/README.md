# HMIS Engine — Agent Organization

This folder defines a small software-development organization as Claude Code
**subagents**, each tailored to *this* codebase (the `hmis-engine-api` Spring Boot
modular monolith + `hmis-engine-web` Angular SPA — a clean-arch rewrite of the
4-year-production legacy **Zana-HMIS**).

Each `*.md` file is one role: YAML frontmatter (`name`, `description`, `tools`) plus a
system prompt. Claude routes work to an agent automatically based on its `description`,
or you can summon one explicitly: *"have the **backend-engineer** implement …"*,
*"ask the **code-reviewer** to review my diff"*.

## The team

| Agent | Role | Reach |
|---|---|---|
| `engineering-manager` | Project / Eng Manager — phasing, prioritization vs `PROCESS.md` §17, coordination | read-only + web |
| `business-analyst` | Domain expert — legacy-process fidelity, acceptance criteria, gap-vs-simplification | read-only + web |
| `solution-architect` | Architect — modulith boundaries, DDD layering, data-model & API design | read-only + web |
| `backend-engineer` | Spring Boot / Java module implementation | full edit + build |
| `frontend-engineer` | Angular 18 features, services, routing | full edit + build |
| `database-engineer` | Flyway migrations, Postgres schema, JPA mappings | full edit + build |
| `qa-engineer` | JUnit/Testcontainers + Karma tests; process verification | full edit + build |
| `devops-engineer` | Docker, docker-compose, AWS deploy scripts, nginx, CI | full edit + build |
| `security-engineer` | AuthN/AuthZ, tenant isolation, id/uid exposure, PII | read + edit + build |
| `code-reviewer` | Tech-lead review gate — correctness, conventions, fidelity | read-only + build |
| `tech-writer` | README/docs, OpenAPI, `PROCESS.md` stewardship | read + edit docs |

## Typical flow of a feature

```
engineering-manager  ──plan──▶  business-analyst  ──acceptance criteria──▶  solution-architect
        │                                                                          │ design
        ▼                                                                          ▼
   backend-engineer ◀──schema──▶ database-engineer        frontend-engineer
        │                                                        │
        └───────────────▶  qa-engineer  (tests + fidelity)  ◀────┘
                                  │
                          security-engineer (authz/tenancy/exposure)
                                  │
                          code-reviewer  ──▶  (merge)  ──▶  devops-engineer (deploy)
                                  │
                             tech-writer (docs)
```

## Shared conventions every agent honors

- **Process fidelity** — architecture and UI modernize; **legacy workflow semantics
  do not** (`PROCESS.md` is canonical; §17 is the coverage map).
- **Dual identifiers** — `id` (BIGSERIAL `Long`, in payloads) + `uid` (ULID, in URLs);
  `id` never appears in a REST path.
- **URI shape** — `/entity/uid/{entityUid}/sub-resource`; frontend service URLs mirror it.
- **Spring Modulith boundaries** — modules talk via published `api`/events, never by
  reaching into another module's `domain`/`infrastructure`; no cycles.
- **Flyway** — new feature ⇒ next `V<n>` migration; editing an existing migration is OK
  only for in-dev corrections (the dev DB is wiped & re-applied from V1).

## Commands (no Maven wrapper; no root POM)

- Backend: from `hmis-engine-api/` → `mvn -q compile`, `mvn -q test`, `mvn -B package`.
- Frontend: from `hmis-engine-web/` → `npm run build`, `npm test`.
- Local DB: `docker compose up -d postgres` (db `hmis_engine`, `hmis`/`hmis`, host 5433).
- QA deploy (bash + AWS CLI v2, from repo root): `bash deploy/aws-up.sh` /
  `aws-redeploy.sh` / `aws-down.sh` (see `DEPLOY_QA.md`).
