# Engineering: modules, testing, CI, migrations and the API

The rules the codebase follows from the first commit. Rule ids start with `ENG`.

Read [README.md](README.md) first. Terms are defined in [`GLOSSARY.md`](../../GLOSSARY.md) and are not redefined here.

## 1. Stack

- **ENG-1** Java 25.
- **ENG-2** Spring Boot 4, with the Spring Security and Spring Framework versions it manages, and Spring Modulith and Spring AI through their own BOMs. The build started on 2026-11-17 on the newest Spring Boot 4 release that Spring Modulith and Spring AI both supported that day:

  | Component | Version | How it is set |
  | --- | --- | --- |
  | Spring Boot | 4.1.2 | parent of `backend/pom.xml` |
  | Spring Framework | 7.0.10 | managed by Spring Boot |
  | Spring Security | 7.1.2 | managed by Spring Boot |
  | Spring Modulith | 2.1.2 | `spring-modulith-bom` |
  | Spring AI | 2.0.2 | `spring-ai-bom` |

  Checked against each project's own sources: Maven Central lists these as the newest releases (Boot 4.2.0, Modulith 2.2.0 and Spring AI 2.1.0 are still release candidates), the Spring AI reference says "Spring AI 2.0.x supports Spring Boot 4.0.x and 4.1.x", and the Modulith 2.1.2 and Spring AI 2.0.2 build files are built on Spring Boot 4.1.2. Renovate updates the Spring Boot, Spring Modulith and Spring AI rows in the pull request that changes them (ENG-52). The owner updates the Spring Framework and Spring Security rows, which follow Spring Boot.
- **ENG-3** PostgreSQL 18, with Flyway (and `flyway-database-postgresql`), Testcontainers and the PostgreSQL driver in the versions Spring Boot manages. Liquibase is not used.
- **ENG-4** springdoc-openapi generates the OpenAPI document.
- **ENG-5** Every container image is built for `linux/arm64`, because production is an ARM machine.
- **ENG-6** The Excel reader and the PDF writer are chosen in the build ticket that first needs them. Neither may depend on Spring.
- **ENG-7** Spring Boot 4 renamed its starters and defaults to Jackson 3, Testcontainers 2 renamed its artifacts and packages, and Spring AI 2.0 broke much of the 1.x API. Only documentation for these versions is followed.
- **ENG-8** The code uses only standard PostgreSQL features, so the database can be moved to a managed service by a dump and a restore ([ADR 0001](../adr/0001-self-hosted-postgresql-on-the-app-machine.md)).

_Sources: [Spring Boot 4 stack versions and compatibility](https://github.com/byanto/niskora/issues/3), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._

## 2. Modules

- **ENG-9** niskora is one bounded context with one `GLOSSARY.md`. A term means the same in every module.
- **ENG-10** The backend is cut into nine Spring Modulith modules under `com.niskora`:

  | Module | Owns | Depends on |
  | --- | --- | --- |
  | `catalogue` | Merchant SKUs, Combination SKUs and components, SPUs, companies, the prefix table, photos | none |
  | `orders` | marketplaces, stores, packages, package lines, component lines, held packages, demand, destinations as exported, buyer personal data | `catalogue` |
  | `stock` | inventory snapshots, stock movements, stock history gaps, stockout days, Weighted Average Cost | `catalogue` |
  | `imports` | uploads, file type detection, translation of BigSeller's formats, the import record and its report, staleness, the stored original files | `catalogue`, `orders`, `stock` |
  | `purchasing` | suppliers, supply sources, the supply source sheet, purchase orders and their documents | `catalogue`, `stock` |
  | `restock` | sales rates, campaign days and restock advice | `catalogue`, `orders`, `stock`, `purchasing`, `imports` |
  | `locations` | places, place aliases and customer location analysis | `orders`, `imports` |
  | `agent` | the agent, its tools and conversations | the public queries of any module, never buyer personal data |
  | `identity` | users and login | none |

- **ENG-11** A tenth, technical module `common` holds only things with no business rule: the business day type and the Jakarta clock, the Problem Details error shape, paging, the build id header (ENG-92) and the fixed parts of the OpenAPI document (ENG-89). A business type stays with its owner, so the Merchant SKU code type is part of the public interface of `catalogue`.
- **ENG-12** Each module owns its REST endpoints. There is no `api` or `web` module that reaches into the others.
- **ENG-13** Dependencies point one way, as in the table, with no cycles. Each module declares the modules it may depend on.
- **ENG-14** A direct call to a module's public interface is the default, for every query and for every command that must succeed or fail with its caller.
- **ENG-15** Events are only for reactions after the fact. They are published through Modulith's event publication registry, so a failed listener is retried. v1 has two:
  1. **Import completed**, published by `imports` with the data set and the business day. `restock` listens and rebuilds its stored sales rates.
  2. **Merchant SKUs added**, published by `catalogue`. `orders` listens and releases the held packages that waited for those codes. `purchasing` listens and gives a new Single SKU its SPU's supply sources.
- **ENG-16** Some things have a single owner, and no other module repeats the work:
  - Only `imports` knows BigSeller's column names, sheet layouts and status spellings. It translates a file into commands that `catalogue`, `orders` and `stock` define in their own words. A later source, such as a marketplace API, would be a second translator beside it.
  - Only `orders` computes demand.
  - Only `stock` computes stockout days.
  - Only `imports` answers whether data is stale.
  - `agent` holds no business data. Each tool calls a public query interface, the same ones the screens use, and no module knows that `agent` exists ([ADR 0005](../adr/0005-the-agent-answers-through-fixed-tools-and-never-writes-sql.md)).
- **ENG-17** Rules live in the domain layer of a module, where they are tested without Spring and without a database. A use case that grows slow to test is a sign that rules have leaked out of the domain layer.
- **ENG-18** Pricing becomes its own module `pricing` after v1, depending only on `catalogue`. v1 creates nothing for it, not even an empty module.

_Sources: [Bounded contexts and module layout](https://github.com/byanto/niskora/issues/6), [Reporting and read model approach](https://github.com/byanto/niskora/issues/15), [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 3. Database

- **ENG-19** One PostgreSQL database, and one schema per module. A module's SQL touches only its own schema. No foreign key crosses a schema. Modules refer to each other's data by business key: Merchant SKU code, Package No, Store Nickname ([ADR 0003](../adr/0003-one-database-schema-per-module.md)).
- **ENG-20** The module that holds a reference checks that the thing exists by asking its owner.
- **ENG-21** Buyer personal data is the one exception to one schema per module. `orders` owns a second schema, `orders_personal`, keyed by Package No and reached only through a separate named interface. A Modulith rule checked on every build forbids `agent`, `locations` and `restock` from depending on that interface.

_Sources: [Bounded contexts and module layout](https://github.com/byanto/niskora/issues/6)._

## 4. Reading data for analysis

- **ENG-22** There are no read models and no reporting module. An analysis reads the owning module's tables live, through that module's public query interface, and lives in the module that owns its question.
- **ENG-23** Counting happens in SQL and rules are applied in code. A query over a large table returns grouped rows, and the business rules (place mapping, countable days, the restock formula, levels, rounding) run in code on that result.
- **ENG-24** Data from several modules is combined in code. The module that owns the large table aggregates it down and hands a small result to the caller, which combines it in memory.
- **ENG-25** Screens and agent tools call the same public query interfaces.
- **ENG-26** Every figure means "as known now": it reflects the current data and the current rules. There is no as-of reporting. The one frozen figure is the working kept on a purchase order line that was made from restock advice.
- **ENG-27** Analysis screens and agent tools answer in under 1 second on ten years of synthetic data (about 1.5 million packages). One automated test checks this.
- **ENG-28** A breach of that budget is answered first with an index or a better query. Only then may the owning module keep a stored summary table, or a module keep its own event-fed copy of another module's data for a single report. Callers see only public query interfaces, so the swap touches no caller.
- **ENG-29** A stored figure that follows from a rule carries a calculation version number. A release that changes the rule sees the mismatch at startup and rebuilds the figure in full by itself.

_Sources: [Reporting and read model approach](https://github.com/byanto/niskora/issues/15)._

## 5. Repository layout and build

- **ENG-30** One repository: `backend/` on Maven with the wrapper committed, `frontend/` on Vite with pnpm, and the Compose files at the root.
- **ENG-31** The whole gate runs locally with one command per side: `./mvnw verify` and `pnpm check`.

_Sources: [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14)._

## 6. Tests

- **ENG-32** Every behaviour is written test-first: it has a test that was seen failing before the code made it pass.
- **ENG-33** The backend has four layers of tests:
  1. **Domain tests.** Plain JUnit, no Spring, no database. Most test-first cycles happen here.
  2. **Use case tests.** One module started alone against a real PostgreSQL 18 in Testcontainers and driven through its public interface.
  3. **Adapter tests.** MockMvc for HTTP adapters (status codes, validation, error format, login), Testcontainers for persistence adapters with non-trivial queries, and file-based tests for the Excel parsers and the PDF writer.
  4. **Application tests.** The whole backend over HTTP with a real login, for a few flows that cross modules.
- **ENG-34** A repository is never mocked and never replaced by an in-memory fake. Fakes exist only for things outside the system: the clock, Object Storage and the AI provider. There is no H2 anywhere ([ADR 0002](../adr/0002-use-case-tests-run-against-real-postgresql.md)).
- **ENG-35** Every database test starts from an empty database and runs all migrations.
- **ENG-36** Test data is small, hand-built, synthetic export files in the test resources: one per export type and one per known oddity (a hub package, a Combination SKU, a masked Lazada address, a code that breaks the convention), with invented Merchant SKUs, buyers and addresses. They are created when the import is built.
- **ENG-37** An optional run reads the local `samples/` folder when it is present. It is skipped in CI.
- **ENG-38** The frontend has component and screen tests with Vitest and Testing Library, with the API faked at the network level by MSW.
- **ENG-39** About ten Playwright journeys run against the full Compose stack, one happy path per capability.
- **ENG-40** Any change that touches a screen is also opened and checked by the coding agent in a real browser, with a screenshot on the pull request. The browser tool is installed on the developer machine and is not in the repository.
- **ENG-41** A flaky test is fixed or deleted. It is never retried automatically.
- **ENG-42** There is no coverage percentage gate. JaCoCo reports coverage.

_Sources: [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14)._

## 7. Module boundaries

- **ENG-43** A test runs `ApplicationModules.verify()` on every build. It fails on a cycle and on any reach into another module's internals.
- **ENG-44** The same test regenerates the module diagram into `docs/`.
- **ENG-45** Rules inside a module are enforced with ArchUnit once they exist.

_Sources: [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14)._

## 8. Merge gate and CI

- **ENG-46** `main` is protected: no direct pushes, pull requests only, and every check green.
- **ENG-47** The checks are: the backend build with all four test layers, module verification, the frontend type check, lint and tests, the Playwright suite, a build of both Docker images without pushing them, the pull request title check, the OpenAPI drift check (ENG-89) and the breaking-change check (ENG-91).
- **ENG-48** There are no git hooks. CI is the only gate, and the project `CLAUDE.md` tells agents to run the local gate before pushing.

_Sources: [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14), [API conventions](https://github.com/byanto/niskora/issues/30)._

## 9. Code style and static analysis

- **ENG-49** Backend: Spotless with google-java-format, Error Prone with NullAway and JSpecify annotations, and compiler warnings as errors.
- **ENG-50** Frontend: TypeScript strict mode, ESLint and Prettier.
- **ENG-51** All of them fail the build. No hosted analysis service is used.

_Sources: [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14)._

## 10. Dependency updates

- **ENG-52** Renovate opens grouped pull requests weekly (the Spring family, the frontend, Docker images and GitHub Actions), each through the full merge gate. Patch and minor updates merge by themselves when green. Major updates and new Spring Boot minors wait for the owner.
- **ENG-53** Docker images and GitHub Actions are pinned by digest. GitHub security alerts stay on.

_Sources: [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14)._

## 11. Branches, commits and pull requests

- **ENG-54** Development is trunk-based: one short-lived branch per build ticket, named after the issue (`42-import-order-export`), and squash-merged, so `main` is linear with one commit per ticket.
- **ENG-55** Pull request titles follow Conventional Commits and are checked by CI. Commits inside a branch are free-form.
- **ENG-56** An agent may open and update a pull request. Only the owner merges.
- **ENG-57** There are no version numbers and no generated changelog yet. A deploy is identified by its commit hash.

_Sources: [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14)._

## 12. Database migrations

- **ENG-58** Schema changes are plain SQL Flyway versioned migrations. There are no Java migrations, and repeatable migrations are only for views.
- **ENG-59** Versions are timestamps, for example `V20261113_1030__add_supplier.sql`.
- **ENG-60** Each module has its own migrations, and they change only its own schema.
- **ENG-61** A migration merged to `main` is never edited. Checksum validation stays on, and `clean` is disabled outside tests.
- **ENG-62** Migrations go forward only, with no undo scripts. Rollback is redeploying the previous image, so each migration must leave the previous app version able to run: add first, and remove in a later release.
- **ENG-63** Hibernate only validates the schema. Flyway migrates when the backend starts.

_Sources: [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14), [Bounded contexts and module layout](https://github.com/byanto/niskora/issues/6)._

## 13. Rules every module shares

- **ENG-64** Code never reads the system time directly. A `Clock` is injected. Moments are stored in UTC, and business dates are computed in `Asia/Jakarta`.
- **ENG-65** Every API error is a Problem Details response (RFC 9457) with a stable error code, which the frontend maps to a translation key.
- **ENG-66** Validation happens at the edges only: HTTP input, uploaded files, the AI provider's answers and configuration. Inner code trusts its types.
- **ENG-67** Buyer personal fields are never logged. A test enforces it.

_Sources: [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14), [API conventions](https://github.com/byanto/niskora/issues/30), [Monitoring and alerting](https://github.com/byanto/niskora/issues/27)._

## 14. API conventions

The `api-and-interface-design` skill from `addyosmani/agent-skills` applies as written, except where a rule below says otherwise. ENG-93 lists the departures.

### Paths and identifiers

- **ENG-68** Every endpoint lives under `/api/<module>/...`, with plural, lowercase, hyphenated nouns: `/api/purchasing/purchase-orders/{id}`. Paths carry no version. The one exception is `/api/health`, an operational path and not a module endpoint: Actuator answers only on an internal management port that is never published, and the proxy forwards this single path to its health endpoint, so no other Actuator endpoint is reachable from outside.
- **ENG-69** Create, read, edit and delete use the plain resource path with `POST`, `GET`, `PATCH` and `DELETE`. A path never carries a verb for one of these four.
- **ENG-70** A state change that is a named domain operation is a verb sub-path, such as `POST /api/purchasing/purchase-orders/{id}/place`, within three limits:
  1. The verb is the last path segment, directly after a resource id.
  2. It is always a `POST`, with `Idempotency-Key` and `If-Match` like any other change.
  3. It is only for an operation that is not a create, read, edit or delete.
- **ENG-71** A thing niskora creates (a purchase order, a supplier, an import, a conversation) is addressed by a UUID. A thing BigSeller names is addressed by its own key (Merchant SKU code, Package No), always URL-encoded as a path segment, because some codes break the code convention.

### Naming

- **ENG-72** JSON fields and query parameters are `camelCase`. Enum values are `UPPER_SNAKE_CASE` (`ORDER_NOW`, `TO_BE_RESENT`); the frontend maps them to translation keys and never shows them raw.
- **ENG-73** A boolean field starts with `is`, `has` or `can`. A moment ends in `At` (`placedAt`). A business date ends in `Date` (`expectedDate`).
- **ENG-74** A response always includes every field. An empty value is an explicit `null`, never left out.
- **ENG-75** Request types and response types are always separate. A value with variants is a discriminated union with a `type` field. No endpoint returns a different shape depending on conditions. Frontend ids are branded types.

### Paged lists

- **ENG-76** Every paged list answers with one shape:

  ```json
  { "data": [], "pagination": { "page": 1, "pageSize": 50, "totalItems": 1234, "totalPages": 25 } }
  ```

- **ENG-77** `page` starts at 1. The page size is fixed at 50 by the server and is not a parameter. The server does all paging, sorting and filtering.
- **ENG-78** `sort` takes a comma-separated list, with a minus for descending: `sort=-orderedAt,code`. `q` is free-text search. Each filter is its own named parameter and may repeat: `status=PLACED&status=DRAFT`.
- **ENG-79** An unknown sort field or filter value is a 400. It is never silently ignored.

### Values

- **ENG-80** Values are written as follows:

  | Value | Form |
  | --- | --- |
  | Money | an object with a decimal string and a currency: `{ "amount": "1250000.00", "currency": "IDR" }` |
  | Quantity | a JSON integer |
  | Other decimal (sales rate, price ratio, margin) | a JSON number |
  | Business date | `2026-11-16` |
  | Moment | ISO 8601 in UTC with `Z`: `2026-11-16T03:15:00Z` |
  | Period | `from` and `to` as business dates, both ends included |

### Status codes and errors

- **ENG-81** Status codes mean:

  | Code | Meaning |
  | --- | --- |
  | 200 | an edit or state change: the full updated resource with its new `ETag` |
  | 201 | a create: the new resource with a `Location` header |
  | 202 | an upload stored, with the import marked "processing" |
  | 204 | a delete, also when the thing is already gone |
  | 400 | the request cannot be read or fails shape validation |
  | 401 | not logged in |
  | 404 | not found |
  | 409 | a conflict with the current state, or a repeat of a request that is still running |
  | 412 | `If-Match` does not match the current version |
  | 422 | a business rule refused, or an `Idempotency-Key` reused with a different body |
  | 428 | a change sent without `If-Match` |

- **ENG-82** A validation error carries an `errors` array with every field error at once. Each entry is `{ "pointer": "/lines/2/quantity", "code": "MUST_BE_POSITIVE", "detail": "..." }`: a JSON Pointer to the field and its own stable code, which the frontend translates like the top-level code.

### Edits

- **ENG-83** Every edit is a `PATCH` with JSON Merge Patch meaning: a field left out is unchanged, and a field sent as `null` is cleared. There is no `PUT`.
- **ENG-84** Purchase order lines are a sub-resource (`POST`, `PATCH` and `DELETE` on `/purchase-orders/{id}/lines/{lineId}`). A whole array of lines is never sent back.

### Repeated requests

- **ENG-85** Every `POST` that creates something or triggers an action requires an `Idempotency-Key` header: a UUID the frontend makes once per user intent and reuses on a retry. Keys are kept for 24 hours.
- **ENG-86** The backend claims the key with a unique constraint in a single insert, never by looking first and inserting after. It stores the key with a hash of the request and the response, and replays the stored response on a repeat. A repeat that arrives while the first request is still running gets a 409. A key reused with a different body gets a 422.

  A repeated upload is caught by the file's SHA-256, as described in [foundation.md](foundation.md).

### Two users editing the same thing

- **ENG-87** Every editable resource has a version, sent as an `ETag` on read and required back as `If-Match` on every change. A mismatch is refused with 412 and nothing is merged. A change without `If-Match` is refused with 428.
- **ENG-88** A purchase order has one version for the whole order, lines included. A line change needs the order's `If-Match` and raises the order's version, so two people editing different lines at the same moment also conflict.

  What the user sees on a conflict is in [frontend.md](frontend.md).

### The contract and how it changes

- **ENG-89** The backend is the source of truth for the API. springdoc generates the OpenAPI document, which is committed, and the TypeScript client types are generated from it. CI regenerates both and fails on any difference.
- **ENG-90** A build ticket works contract first: it writes the controller signature and the request and response types, regenerates the OpenAPI document and the client, and only then writes the behaviour.
- **ENG-91** The API changes additively within one release. A removal or a rename takes two releases, the same rule the migrations follow. CI compares the generated OpenAPI document with the one on `main` and fails on a breaking change, unless the pull request is marked as the planned removal step.
- **ENG-92** Every response carries the backend's build id in a header, so the frontend can tell when a new version is running.

### Where niskora departs from the skill

- **ENG-93** These departures are deliberate:

  | Topic | The skill | niskora |
  | --- | --- | --- |
  | Error body | `{ error: { code, message } }` | Problem Details (RFC 9457) |
  | Verbs in paths | none | a verb sub-path for a named domain operation, within the limits of ENG-70 |
  | Sorting | `sortBy` and `sortOrder` | one `sort` parameter |
  | Page size | a `pageSize` parameter | fixed at 50 by the server |
  | Validation status | 422 for all validation | 400 for shape, 422 for a refused business rule |
  | Version mismatch | 409 | 412, the status for a failed `If-Match` |

_Sources: [API conventions](https://github.com/byanto/niskora/issues/30), [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14), [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26)._

## 15. Definition of done for a build ticket

- **ENG-94** A build ticket is done when all of these hold:
  1. It was written test-first (ENG-32).
  2. The whole gate passes locally (ENG-31).
  3. `GLOSSARY.md`, `docs/adr/` and this spec are current.
  4. Screen changes were checked in a real browser (ENG-40).
  5. No real business data is in the code, the tests, the commits or the pull request.
  6. For a ticket that adds or changes an endpoint, the verification checklist of the `api-and-interface-design` skill passes.

_Sources: [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14), [API conventions](https://github.com/byanto/niskora/issues/30), [Assemble the v1 spec and build order](https://github.com/byanto/niskora/issues/29)._
