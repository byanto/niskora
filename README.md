# niskora

A business brain that sits beside [BigSeller](https://www.bigseller.com/) for a multi-store Indonesian e-commerce seller.

BigSeller syncs orders and stock across marketplaces and does that well. It does not tell you what to reorder, from whom, and when, and it does not show where your buyers are. niskora takes BigSeller's own exports and builds those answers on top.

## The business it is built for

niskora is built for one real business, mine, and used by the people who run it every day:

- 12 marketplace stores (Shopee, Lazada, TikTok Shop, Blibli) plus manual orders
- more than 10,000 active Merchant SKUs
- 300 to 500 orders a day from one warehouse
- suppliers both local and in China

Today the knowledge needed to run it is spread over BigSeller, several Google Sheets and people's heads. niskora is where it comes together.

## What v1 does

1. **Import foundation.** A person uploads BigSeller's Excel exports (orders, inventory, stock movements, the Merchant SKU list). niskora turns them into a clean catalogue, order history and stock history, and reports exactly what each import changed. BigSeller has no free API, so there is no scraping and no automation; data up to a day old is fine.
2. **Restock advice and purchase orders.** For every Single SKU: whether to reorder now and how many, from a sales rate that leaves out stockout days and campaign days, with the working shown on every line. Advice becomes a purchase order per supplier, with printable documents, and received quantities are read back from BigSeller's stock movements.
3. **Customer location analysis.** Where packages go, by province, city and district, for aiming place-targeted ads.
4. **A read-only agent.** Answers questions in plain language from niskora's own data, through a fixed set of tools. It never writes SQL and never sees buyer data.

niskora complements BigSeller and never replaces it. BigSeller stays the source of truth for order sync and stock quantities.

## Status

In progress. The design for v1 is nearly complete and the implementation is starting.

- The domain language is settled in [`GLOSSARY.md`](GLOSSARY.md).
- Architecture decisions are recorded in [`docs/adr/`](docs/adr/).
- Every design decision was worked out as one ticket on a [decision map](https://github.com/byanto/niskora/issues/1), each closed with a written resolution. The map ends at the v1 spec.

## How it is built

- **Java 25, Spring Boot 4, Spring Modulith.** One modular monolith, no microservices. Nine modules with one-way dependencies: `catalogue`, `orders`, `stock`, `imports`, `purchasing`, `restock`, `locations`, `agent`, `identity`.
- **PostgreSQL 18**, one schema per module and no foreign keys across schemas ([ADR 0003](docs/adr/0003-one-database-schema-per-module.md)). Plain SQL, forward-only Flyway migrations.
- **Tests against the real database.** Use case tests run on real PostgreSQL, with no mocked or fake repositories ([ADR 0002](docs/adr/0002-use-case-tests-run-against-real-postgresql.md)).
- **React** frontend as a separate app, with an OpenAPI contract generated from the backend.
- **Docker Compose** locally and in production, on a single Oracle Cloud machine ([ADR 0001](docs/adr/0001-self-hosted-postgresql-on-the-app-machine.md)).

When a technical choice has to be made, development cost gets little weight. Quality, simplicity, robustness and long-term maintainability come first.

## Running it locally

You need Docker, JDK 25 and Node 24 with Corepack (for pnpm).

- `docker compose up --build` starts PostgreSQL 18, the backend and Caddy. Open <http://localhost:8000>.
- `pnpm dev` in `frontend/` starts the Vite dev server on <http://localhost:5173>. It forwards `/api` to the backend from Compose.
- The local gate is `./mvnw verify` in `backend/` (it needs Docker for Testcontainers) and `pnpm check` in `frontend/`.
- `./mvnw spotless:apply` in `backend/` fixes the formatting that the gate checks. `./mvnw verify` also regenerates the module diagram in `docs/modules/` and writes a coverage report to `backend/target/site/jacoco/`.

## How I work on it

niskora is developed with an AI coding agent (Claude Code) in the loop. The agent does the legwork: research, drafting, and writing code. I make the decisions, and I read, question and approve every line before it lands. The decision map, the glossary and the ADRs exist so that both of us work from the same written ground truth instead of from memory.

## Data

The repository holds no real business data. Real exports live in a gitignored `samples/` folder, tests use synthetic export files, and buyer personal data is kept in its own schema that the analysis modules and the agent cannot reach.
