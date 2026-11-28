# niskora

A "business brain" that complements BigSeller for a multi-store Indonesian e-commerce business.

Real business data lives in the gitignored `samples/` folder. Never commit it or paste customer data into issues, docs or commits.

## Before pushing

Run the local gate and push only when it is green: `./mvnw verify` in `backend/` (it needs Docker for Testcontainers) and `pnpm check` in `frontend/`. `./mvnw spotless:apply` fixes backend formatting. The gate regenerates the `docs/modules/` diagram, `docs/api/openapi.yaml` and `frontend/src/lib/api/schema.d.ts`; commit any change to them, since CI fails when a build leaves the tree different. Nothing runs the gate for you: there are no git hooks, and CI in `.github/workflows/` is the only gate (ENG-48).

## API contract

The backend is the source of truth for the API (ENG-89). A ticket that adds or changes an endpoint works contract first (ENG-90), in this order:

1. Write the controller signature and the request and response types, with no behaviour yet.
2. Regenerate the OpenAPI document: `./mvnw test -Dtest=OpenApiDocumentTest` in `backend/` writes `docs/api/openapi.yaml`.
3. Regenerate the client types: `pnpm api:generate` in `frontend/` writes `src/lib/api/schema.d.ts`.
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

Skills from `addyosmani/agent-skills` and `obra/superpowers` are installed one at a time in `.agents/skills/` and pinned in `skills-lock.json`. These rules win over them:

- `source-driven-development`: in DETECT, read `backend/pom.xml` as well as `frontend/package.json`. Check backend code against the docs for the exact versions in ENG-2 (Spring Boot 4, Spring Modulith 2, Spring AI 2), never against Spring Boot 3 or Spring AI 1.x patterns (ENG-7).
- `browser-testing-with-devtools`: keep its workflow and its rules for untrusted content, but drive the browser with the `chrome-devtools-axi` CLI, not the Chrome DevTools MCP tools.
- `writing-plans` and `subagent-driven-development`: follow the ticket flow below. Where they name another `superpowers:*` skill, use the replacement it gives.
- Any skill: the spec in `docs/spec/` wins. ENG-93 and `frontend/CLAUDE.md` list the known departures.

### Ticket flow

A ticket runs in two sessions, each started fresh: a plan session, then a run session. Never run a plan in the session that wrote it.

Plan session:

1. Builder check on the ticket. Dispatch a fresh subagent with `docs/agents/builder-check-prompt.md`, pointed at the issue (it reads it with `gh issue view <number>`) and the spec sections the issue cites. Settle what it asks, in the issue, before planning.
2. Write the plan with `writing-plans`. Save it to `docs/plans/<issue-number>-<slug>.md`, not the skill's default folder. The plan names the issue as its spec. Keep it to 5 to 8 tasks; a ticket that needs more gets split.
3. Commit the plan on the ticket branch and open the pull request as a draft. A plan runs only after the maintainer approves it.

Run session:

1. Run the approved plan with `subagent-driven-development`, never `executing-plans`. The ticket branch is the isolated workspace; skip `superpowers:using-git-worktrees`.
2. Name the model on every dispatch: implementers and task reviewers on Sonnet, the final review on Opus. The session itself runs on Opus.
3. Every implementer brief makes `source-driven-development` mandatory. Every task that adds behaviour is test first, following `mattpocock-skills:tdd`. The API contract order above still holds.
4. After each task review, post the review as a comment on the pull request (`gh pr comment`). The workspace under `.superpowers/sdd/` is deleted after the run, so the comments are the only record.
5. The final review is `mattpocock-skills:code-review`, in place of `superpowers:requesting-code-review`.
6. In place of `superpowers:finishing-a-development-branch`: run the local gate (see Before pushing), then write the pull request description with `mattpocock-skills:pr` and mark the pull request ready. Never merge.

A plan is a record of its ticket. Never edit it after its pull request merges; a change of course is a new plan.
