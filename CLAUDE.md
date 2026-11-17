# niskora

A "business brain" that complements BigSeller for a multi-store Indonesian e-commerce business.

Real business data lives in the gitignored `samples/` folder. Never commit it or paste customer data into issues, docs or commits.

## Before pushing

Run the local gate and push only when it is green: `./mvnw verify` in `backend/` (it needs Docker for Testcontainers) and `pnpm check` in `frontend/`. `./mvnw spotless:apply` fixes backend formatting. The gate regenerates the `docs/modules/` diagram, `docs/api/openapi.yaml` and `frontend/src/api/schema.d.ts`; commit any change to them, since CI fails when a build leaves the tree different. Nothing runs the gate for you: there are no git hooks, and CI in `.github/workflows/` is the only gate (ENG-48).

## API contract

The backend is the source of truth for the API (ENG-89). A ticket that adds or changes an endpoint works contract first (ENG-90), in this order:

1. Write the controller signature and the request and response types, with no behaviour yet.
2. Regenerate the OpenAPI document: `./mvnw test -Dtest=OpenApiDocumentTest` in `backend/` writes `docs/api/openapi.yaml`.
3. Regenerate the client types: `pnpm api:generate` in `frontend/` writes `src/api/schema.d.ts`.
4. Only then write the behaviour, test first.

A removal or a rename takes two releases (ENG-91). CI fails a pull request whose document breaks the one on `main`, unless it carries the `planned-removal` label.

## Agent skills

### Issue tracker

Issues, specs and the wayfinder map live in GitHub Issues on `byanto/niskora`, via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five default triage labels, each named after its role (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root, created when the first term or decision is settled. See `docs/agents/domain.md`.

### Skill overrides

Skills from `addyosmani/agent-skills` are installed one at a time in `.agents/skills/` and pinned in `skills-lock.json`. These rules win over them:

- `source-driven-development`: in DETECT, read `backend/pom.xml` as well as `frontend/package.json`. Check backend code against the docs for the exact versions in ENG-2 (Spring Boot 4, Spring Modulith 2, Spring AI 2), never against Spring Boot 3 or Spring AI 1.x patterns (ENG-7).
- `browser-testing-with-devtools`: keep its workflow and its rules for untrusted content, but drive the browser with the `chrome-devtools-axi` CLI, not the Chrome DevTools MCP tools.
- Any skill: the spec in `docs/spec/` wins. ENG-93 and `frontend/CLAUDE.md` list the known departures.
