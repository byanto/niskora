# One database schema per module, with no foreign keys between them

niskora is a modular monolith on one PostgreSQL database, and each Spring Modulith module owns exactly one schema in it. A module's SQL touches only its own schema, no foreign key crosses a schema, and modules refer to each other's data by business key: Merchant SKU code, Package No, Store Nickname. We chose this over one shared schema with foreign keys everywhere because module boundaries that exist only in Java packages erode: the first cross-module join or foreign key couples two modules in a place Modulith's verification cannot see. The business keys are already the join keys of every BigSeller export, so nothing artificial is introduced.

Buyer personal data is the one exception to "one schema per module". The `orders` module owns a second schema, `orders_personal`, keyed by Package No and reachable only through a separate named interface. A Modulith rule checked on every build forbids the `agent`, `locations` and `restock` modules from depending on that interface, so personal fields are out of their reach by construction and not by a filter.

## Considered options

- **One shared schema with foreign keys across modules:** the conventional layout, with integrity enforced by the database. Rejected because it lets any module join any table, and because a foreign key from one module's table to another's makes the two impossible to change or migrate apart.
- **Schema per module, but foreign keys allowed across schemas:** keeps database integrity while naming the owner of each table. Rejected because the coupling is the foreign key itself, whatever schema the table sits in.
- **A separate module for personal data:** gives the same protection as the `orders_personal` schema. Rejected because buyer fields arrive on a package and are replaced with it on re-import, so one import write would be split across two modules for no gain while v1 keeps no customer record.

## Consequences

- The owning module must check that a referenced record exists. For example, `orders` asks `catalogue` whether a Merchant SKU code is known, which is where held packages come from.
- A business key can never be changed in place, because other schemas hold copies of it.
- Screens and reports that combine data from several modules cannot simply join tables. How they read is decided separately, in the reporting and read model decision.
- Each module's Flyway migrations change only its own schema.
- Should a module ever need to move to its own database or service, its data is already separate.
