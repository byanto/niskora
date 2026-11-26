# Platform: how niskora runs, login and users, stored files and logs

How niskora is packaged and released, who can get in, where uploaded files are kept, and what it offers a watcher outside it. Rule ids start with `PLT`.

Read [README.md](README.md) first. Terms are defined in [`GLOSSARY.md`](../../GLOSSARY.md) and are not redefined here.

How a particular installation is deployed, backed up and watched is not part of niskora, and is not in this repository. Section 2 is what every installation can rely on.

## 1. Runtime

- **PLT-1** niskora runs on one ARM machine as one Docker Compose stack: Caddy, the Spring Boot backend and PostgreSQL 18.
- **PLT-2** There are two environments: local Docker Compose and production. There is no staging in v1.
- **PLT-3** Caddy serves the built React app and forwards `/api` to the backend, so the browser sees a single origin.
- **PLT-4** PostgreSQL runs as a container in that stack, with its data on the machine's disk. No managed database is used ([ADR 0001](../adr/0001-self-hosted-postgresql-on-the-app-machine.md)).
- **PLT-5** Uploaded files and photos are kept in OCI Object Storage, never on the machine's disk.
- **PLT-6** The stack serves plain HTTP and publishes no port to the internet. TLS ends in front of it, so niskora is always reached over HTTPS and its session cookie can be `Secure` (PLT-12).

_Sources: [Deployment, login and roles](https://github.com/byanto/niskora/issues/12), [Spring Boot 4 stack versions and compatibility](https://github.com/byanto/niskora/issues/3), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28), [Which parts of step 1 live in the private operations repository](https://github.com/byanto/niskora/issues/76)._

## 2. Releases and what an installation relies on

- **PLT-7** GitHub Actions runs the tests on every push. On a merge to `main` it builds `linux/arm64` images, pushes them to GitHub's container registry as public packages and tags them by commit.
- **PLT-8** Flyway migrates when the backend starts.
- **PLT-9** An image can always be replaced by the one before it. The migration rules in [engineering.md](engineering.md) keep the previous version able to run on a migrated database, so rolling back is running the previous image.
- **PLT-10** The backend reads every secret and every installation setting from its environment. No secret is ever in the repository or in an issue.
- **PLT-11** An installation relies on these, and only on these. Changing one is a change to this rule, named in its pull request:

  | What | Contract |
  | --- | --- |
  | Images | `ghcr.io/byanto/niskora-backend:<commit>` and `ghcr.io/byanto/niskora-frontend:<commit>`, both built from the same commit. |
  | Shape | The services and wiring of [`compose.yaml`](../../compose.yaml): the frontend image listens on port 80 and reaches the backend as `backend` on port 8080 (the API) and 8081 (the management port). |
  | Health | `GET /api/health` answers `{"status":"UP"}` with 200, or `DOWN` with 503, and nothing more (PLT-31). |
  | Database | PostgreSQL 18, reached through `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. |
  | First account | `java -jar app.jar create-first-user --login-name=<login name> --full-name="<full name>"` in the backend container, with the password on standard input (PLT-18). |
  | Logs | One JSON object per line on the backend's stdout (PLT-29). |

  A later rule that needs a new secret or setting adds its row here: the Gemini API key (AGT-23), the Object Storage credentials (PLT-5), the real company prefix table (FND-21) and the orders import ping address (PLT-27).

_Sources: [Deployment, login and roles](https://github.com/byanto/niskora/issues/12), [Create a Gemini API key and read the free tier limits](https://github.com/byanto/niskora/issues/22), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36), [Which parts of step 1 live in the private operations repository](https://github.com/byanto/niskora/issues/76)._

## 3. Login and users

- **PLT-12** Accounts live in niskora: a username and a password hashed with Argon2, with a server-side session in a cookie that is `Secure`, `HttpOnly` and `SameSite=Lax`. CSRF protection stays on.
- **PLT-13** There is no self-signup and no reset by email. v1 sends no mail.
- **PLT-14** Each person has one account, and accounts are never shared, so that imports, purchase orders and corrections record who made them.
- **PLT-15** There are no roles in v1. Every [user](../../GLOSSARY.md#people) can see and do everything, the buyer fields on a package's page included.
- **PLT-16** A Users screen lets any user create an account, reset another user's password, and disable or enable another user's account.
- **PLT-17** Any user can change their own password, giving their current one. A wrong current password counts as a failed login (PLT-19).
- **PLT-18** The first account is created with a command on the server.
- **PLT-19** Repeated failed logins are slowed down.
- **PLT-20** A disabled account cannot log in, and its open sessions end at once. An account is never deleted, so everything it did keeps its name. Nobody can disable their own account, so at least one account can always log in.

_Sources: [Deployment, login and roles](https://github.com/byanto/niskora/issues/12), [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [Main screens and navigation](https://github.com/byanto/niskora/issues/25), [Customer location analysis](https://github.com/byanto/niskora/issues/10), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._

## 4. Stored files

- **PLT-21** Every uploaded file is kept in its original form in Object Storage.
- **PLT-22** A daily upload is deleted after 1 year by the bucket's lifecycle rule. Each daily order export repeats the 90 days before it, so the old files are nearly all duplicates.
- **PLT-23** The files of the history load are kept forever.
- **PLT-24** The stored originals are what allows the past to be rebuilt by hand, should that ever be needed.
- **PLT-25** A user marks an import as part of the history load when uploading it. Its file is then stored under a separate prefix that the lifecycle rule does not touch.

_Sources: [Deployment, login and roles](https://github.com/byanto/niskora/issues/12), [Monitoring and alerting](https://github.com/byanto/niskora/issues/27), [Reporting and read model approach](https://github.com/byanto/niskora/issues/15)._

## 5. Faults, logs and health

- **PLT-26** Faults have two audiences. Business conditions (stale data, held packages, a rejected file, the agent's limit reached) show in the app only, on Home. Technical faults are for the owner alone, outside the app, because the operator cannot act on them. niskora itself sends no alerts: a watcher outside it reads its health (PLT-31), its logs (PLT-28) and the ping below.
- **PLT-27** Each time an orders import succeeds, the backend pings an address read from its environment, so a watcher outside niskora notices when imports stop. With no address set, it pings nothing.
- **PLT-28** Every unexpected exception is logged at ERROR, so a watcher reading the logs learns of it. There is no error tracker in v1, so no third party receives data from a system that holds buyer details.
- **PLT-29** The backend logs structured JSON to stdout.
- **PLT-30** Buyer personal fields are never logged.
- **PLT-31** There is no metrics store in v1. Spring Boot Actuator health is exposed, and only a bare up or down is visible from outside.

_Sources: [Monitoring and alerting](https://github.com/byanto/niskora/issues/27)._
