# Platform: login, users, deployment, backups and monitoring

How v1 runs, who can get in, and how the owner learns that something is wrong. Rule ids start with `PLT`.

Read [README.md](README.md) first. Terms are defined in [`GLOSSARY.md`](../../GLOSSARY.md) and are not redefined here.

## 1. Machine

- **PLT-1** Production is the OCI machine `instance-k3s`: ARM (`VM.Standard.A1.Flex`), 2 OCPUs, 12 GB RAM, a 100 GB boot disk, in the Singapore region, on Ubuntu 26.04. Nothing else runs on it in Docker.
- **PLT-2** The owner's second OCI machine is not part of v1, except as the place where backup restores are tested.
- **PLT-3** There are two environments: local Docker Compose and production. There is no staging in v1.

_Sources: [Deployment, login and roles](https://github.com/byanto/niskora/issues/12), [Spring Boot 4 stack versions and compatibility](https://github.com/byanto/niskora/issues/3)._

## 2. Packaging

- **PLT-4** Production is one Docker Compose stack with the same shape as local development: Caddy, the Spring Boot backend, PostgreSQL 18 and a Cloudflare Tunnel connector.
- **PLT-5** Caddy serves the built React app and forwards `/api` to the backend, so the browser sees a single origin.
- **PLT-6** PostgreSQL runs as a container in that stack, with its data on the machine's boot disk. No managed database is used ([ADR 0001](../adr/0001-self-hosted-postgresql-on-the-app-machine.md)).
- **PLT-7** Uploaded files and photos are kept in OCI Object Storage, never on the machine's disk.

_Sources: [Deployment, login and roles](https://github.com/byanto/niskora/issues/12), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 3. Reachability and secrets

- **PLT-8** niskora is reached at `niskora.budiyanto.com` through its own Cloudflare Tunnel, separate from any other connector on the machine. No web port is open to the internet.
- **PLT-9** SSH stays reachable with key-only login, and passwords are disabled.
- **PLT-10** Deployments use a separate SSH key that is restricted to the deploy command.
- **PLT-11** The unused rpcbind service on the machine is switched off.
- **PLT-12** Secrets live in one file on the machine that only the service account can read: the database password, the session key, the Cloudflare Tunnel token, the Gemini API key, the OCI Object Storage credentials, the Cloudflare R2 credentials, the backup encryption key, the Healthchecks.io ping addresses and the real company prefix table (FND-21). GitHub holds only the deploy key. No secret is ever in the repository or in an issue.

_Sources: [Deployment, login and roles](https://github.com/byanto/niskora/issues/12), [Create a Gemini API key and read the free tier limits](https://github.com/byanto/niskora/issues/22), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._

## 4. Delivery

- **PLT-13** GitHub Actions runs the tests on every push. On a merge to `main` it builds `linux/arm64` images, pushes them to GitHub's container registry and tags them by commit.
- **PLT-14** Deployment is manual. The owner starts a workflow that connects over SSH and restarts the stack on the chosen tag.
- **PLT-15** Flyway migrates when the backend starts, and a health check follows.
- **PLT-16** Rollback is redeploying the previous tag. The migration rules in [engineering.md](engineering.md) keep the previous version able to run.

_Sources: [Deployment, login and roles](https://github.com/byanto/niskora/issues/12)._

## 5. Login and users

- **PLT-17** Accounts live in niskora: a username and a password hashed with Argon2, with a server-side session in a cookie that is `Secure`, `HttpOnly` and `SameSite=Lax`. CSRF protection stays on.
- **PLT-18** There is no self-signup and no reset by email. v1 sends no mail.
- **PLT-19** Each person has one account, and accounts are never shared, so that imports, purchase orders and corrections record who made them.
- **PLT-20** There are no roles in v1. Every [user](../../GLOSSARY.md#people) can see and do everything, the buyer fields on a package's page included.
- **PLT-21** A Users screen lets any user create an account, reset another user's password, and disable or enable another user's account.
- **PLT-22** Any user can change their own password, giving their current one. A wrong current password counts as a failed login (PLT-24).
- **PLT-23** The first account is created with a command on the server.
- **PLT-24** Repeated failed logins are slowed down.
- **PLT-45** A disabled account cannot log in, and its open sessions end at once. An account is never deleted, so everything it did keeps its name. Nobody can disable their own account, so at least one account can always log in.

_Sources: [Deployment, login and roles](https://github.com/byanto/niskora/issues/12), [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [Main screens and navigation](https://github.com/byanto/niskora/issues/25), [Customer location analysis](https://github.com/byanto/niskora/issues/10), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._

## 6. Backups and stored files

- **PLT-25** Every night an encrypted dump of the database goes to an OCI Object Storage bucket. 7 daily, 4 weekly and 6 monthly copies are kept.
- **PLT-26** The worst-case loss is one day of data. There is no point-in-time recovery.
- **PLT-27** Every week the dump is also copied to Cloudflare R2, where 4 copies are kept, so that losing the OCI account does not lose both the machine and its backups.
- **PLT-28** Once a month the latest dump is restored on the second machine to prove that it works.
- **PLT-29** Every uploaded file is kept in its original form in Object Storage.
- **PLT-30** A daily upload is deleted after 1 year by a bucket lifecycle rule. Each daily order export repeats the 90 days before it, so the old files are nearly all duplicates.
- **PLT-31** The files of the history load are kept forever.
- **PLT-32** The stored originals are what allows the past to be rebuilt by hand, should that ever be needed.
- **PLT-33** A user marks an import as part of the history load when uploading it. Its file is then stored under a separate prefix that the lifecycle rule does not touch.

_Sources: [Deployment, login and roles](https://github.com/byanto/niskora/issues/12), [Monitoring and alerting](https://github.com/byanto/niskora/issues/27), [Reporting and read model approach](https://github.com/byanto/niskora/issues/15)._

## 7. Monitoring and alerting

- **PLT-34** Faults have two audiences. Business conditions (stale data, held packages, a rejected file, the agent's limit reached) show in the app only, on Home. Technical faults go to the owner alone, outside the app, because the operator cannot act on them.
- **PLT-35** Healthchecks.io, on its hosted free tier, is the only watcher. Each job pings when it succeeds, and a missing ping raises the alert, so a dead machine is noticed from outside.
- **PLT-36** Alerts go to a private Discord channel through Healthchecks.io's own integration, with its email as the second channel. niskora itself sends nothing.
- **PLT-37** There are eight checks:

  | No. | Check | How often |
  | --- | --- | --- |
  | 1 | The app is reachable end to end: a timer on the machine fetches its own public health URL through Cloudflare and the tunnel, and pings on success. The alert comes after 10 minutes of silence. | every 5 minutes |
  | 2 | The nightly database dump was made and uploaded to Object Storage. | nightly |
  | 3 | The dump was copied to Cloudflare R2. | weekly |
  | 4 | The restore test on the second machine passed. | monthly |
  | 5 | The boot disk is under 80% full. | daily |
  | 6 | Object Storage usage is under 80% of the free allowance, checked by the nightly backup job. | nightly |
  | 7 | There were no ERROR log lines in the last 24 hours. | daily |
  | 8 | An orders import succeeded within the last 3 days. | daily |

- **PLT-38** Check 7 is how the owner learns of an unexpected exception. There is no error tracker in v1, so no third party receives data from a system that holds buyer details.
- **PLT-39** These are not watched: a failed deploy (GitHub reports it to the owner, who started it, and a broken stack trips check 1), a failed copy of an upload to Object Storage (the import fails and shows in the app), the agent's quota (shown in the app), and CPU, memory, certificates and response times.
- **PLT-40** A runbook has one section per check: what it means, the first command to run, and how to restore from a dump. It lives in the owner's private operations repository, not in this one, because it describes the owner's installation rather than niskora itself. Each check's description in Healthchecks.io links to its section.

_Sources: [Monitoring and alerting](https://github.com/byanto/niskora/issues/27)._

## 8. Logs and metrics

- **PLT-41** The backend logs structured JSON to stdout. Docker's journald driver collects it, so logs survive a redeploy. Logs are kept for 30 days with a 2 GB cap and are read over SSH with `journalctl`.
- **PLT-42** Buyer personal fields are never logged.
- **PLT-43** There is no metrics store in v1. Spring Boot Actuator health is exposed, and only a bare up or down is visible from outside. OCI's built-in instance charts cover CPU, memory and disk.
- **PLT-44** The Object Storage allowance that check 6 measures against is the figure the owner reads from the OCI console.

_Sources: [Monitoring and alerting](https://github.com/byanto/niskora/issues/27)._
