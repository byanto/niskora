# niskora

A "business brain" that complements BigSeller for a multi-store Indonesian e-commerce business.

Real business data lives in the gitignored `samples/` folder. Never commit it or paste customer data into issues, docs or commits.

## Before pushing

Run the local gate and push only when it is green: `./mvnw verify` in `backend/` (it needs Docker for Testcontainers) and `pnpm check` in `frontend/`. Nothing runs it for you: there are no git hooks, and CI in `.github/workflows/` is the only gate (ENG-48).

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
