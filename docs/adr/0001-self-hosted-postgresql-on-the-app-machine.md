# Self-hosted PostgreSQL on the app machine

niskora runs PostgreSQL 18 as a container in the same Docker Compose stack as the backend, on the same OCI machine, with its data on that machine's boot disk. We chose this over Neon and OCI's managed PostgreSQL because the load is tiny (300-500 orders a day, a handful of users), production then has the same shape as local development, the app depends on one provider and no network hop, and customer personal data stays with one company. Durability comes from a nightly encrypted dump to OCI Object Storage with a monthly restore test, which fits the accepted "data up to a day old" bound.

## Considered options

- **Neon (free tier):** managed backups and branching, but 1 GB per project is a tight fit for daily stock snapshots, idle databases suspend, and every query crosses the internet to a second provider.
- **OCI managed PostgreSQL:** backups and upgrades handled by Oracle, but it is paid and solves no problem this workload has.
- **A dedicated block volume for the data:** cleaner separation from the boot disk, but OCI's smallest block volume is 50 GB, which would use up the rest of the owner's free storage allowance for a database expected to stay within a few GB.

## Consequences

- We own PostgreSQL upgrades and the backup job. A backup failure must be noticed, so alerting on it belongs to the operations design.
- The worst-case loss is one day of data. Point-in-time recovery is not available.
- Moving to Neon or a managed service later is a dump, a restore and a connection-string change, provided the code uses only standard PostgreSQL features and no extensions the target lacks.
