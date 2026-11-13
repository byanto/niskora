# Use case tests run against real PostgreSQL

Every test that exercises a use case starts one module and drives it through its public interface against a real PostgreSQL 18 in Testcontainers. Repositories are never mocked and never replaced by in-memory fakes. We chose this over the classic hexagonal style of `Map`-backed repositories because the riskiest logic in niskora, the imports, is database-shaped (replace a package's lines as a set, merge stock movements on a natural key, never delete on absence), and a fake would re-implement exactly the part most likely to be wrong. Fast tests still exist: the rules live in the domain layer and are tested with plain JUnit, with no repository of any kind.

Fakes are used only for things outside the system: the clock, Object Storage and the AI provider.

## Considered options

- **In-memory repositories with contract tests:** the fastest use case tests, and a sound approach when one shared contract test runs against both the fake and the JPA repository. Rejected because every repository is then written and maintained twice, and the speed it buys does not matter for a suite of this size.
- **In-memory repositories without contract tests:** rejected outright. A `Map` enforces no unique or foreign keys, does not roll back a failed transaction and does not sort or filter like SQL, so tests pass while production fails.
- **Mocked repositories:** rejected because such tests check which methods are called, not what the system ends up doing, and break on every refactor.
- **Each module chooses:** rejected because one rule is easier to follow, for people and for coding agents, than a choice made module by module.

## Consequences

- Use case tests need Docker running, locally and in CI, and a full run is seconds slower than it would be with fakes.
- Logic that can be expressed without the database belongs in the domain layer, where it is tested in microseconds. A slow suite is a sign that rules have leaked into use cases.
- Every database test starts from an empty database and runs all Flyway migrations, so the migration chain is proven on every build.
- If the suite ever becomes too slow, in-memory repositories with contract tests can be added for one module without changing the others.
