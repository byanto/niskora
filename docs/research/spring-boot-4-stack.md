# Spring Boot 4 stack: versions and compatibility

Research for issue #3. Everything below was fetched on **2026-11-12**. Version numbers come from Maven Central metadata, published POM files, the `api.spring.io` project API and the official docs, not from memory. Items that could not be verified are listed in [Not verified](#not-verified).

## Short answer

Start on **Spring Boot 4.1.2** with **Spring Modulith 2.1.2**, **Spring AI 2.0.2** and **Spring Security 7.1.2** (the last one is managed by Boot). All three were released on 2026-10-22/23 and are built against Boot 4.1.2. Java 21 works; Java 25 is the better target for a long-lived project. Every library on the list has a release that fits Boot 4.1, and the Oracle ARM machines only require that Docker images are built for `linux/arm64`.

Two things to know before starting:

- **Boot 4.2.0 is due on 2026-11-19**, one week from now. Its partners (Modulith 2.2, Spring AI 2.1, Security 7.2, Framework 7.1) are still release candidates today. Start on 4.1.2 and move to 4.2 once the third-party libraries have caught up.
- **The Oracle Always Free ARM allowance is 2 OCPUs and 12 GB of memory in total** according to Oracle's docs today, not the 4 OCPUs and 24 GB often quoted.

## 1. Spring Boot releases and support

Source: [api.spring.io generations](https://api.spring.io/projects/spring-boot/generations) and [releases](https://api.spring.io/projects/spring-boot/releases) (the data behind the support table on [spring.io/projects/spring-boot](https://spring.io/projects/spring-boot#support)), cross-checked against [Maven Central](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/).

| Line | Latest release | OSS support ends | Commercial support ends |
|---|---|---|---|
| 3.4.x | (none listed as supported) | 2025-12-31 (ended) | 2026-12-31 |
| 3.5.x | 3.5.16 | **2026-06-30 (ended)** | 2032-06-30 |
| 4.0.x | 4.0.9 | 2026-12-31 | 2027-12-31 |
| **4.1.x** | **4.1.2** (current, published 2026-10-22) | 2027-07-31 | 2028-07-31 |
| 4.2.x | 4.2.0-RC2 (GA milestone due 2026-11-19) | 2027-12-31 | 2028-12-31 |

- The API marks **4.1.2** as the current release. The [GitHub release](https://github.com/spring-projects/spring-boot/releases/tag/v4.1.2) is dated 2026-10-22.
- **Free support for the whole 3.x line has ended.** 3.5 is the last 3.x minor; it only gets fixes for paying customers now (until 2032-06-30).
- 4.0.x loses free support in seven weeks, so it is not a sensible starting point.
- The 4.2.0 date comes from the [GitHub milestones](https://github.com/spring-projects/spring-boot/milestones), which also list 4.1.3 and 4.0.10 for 2026-11-19.
- Policy ([Supported Versions wiki](https://github.com/spring-projects/spring-boot/wiki/Supported-Versions)): "Minor versions will be supported for at least 12 months", and a new minor ships "every six months (in May and November)". A project on Boot 4 should expect one minor upgrade every six months.

## 2. Spring Modulith, Spring AI and Spring Security

| Project | Pairs with Boot 4.1 | Evidence |
|---|---|---|
| Spring Security | **7.1.2** | Managed by `spring-boot-dependencies` 4.1.2 ([POM](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom)). No version to declare. |
| Spring Framework | 7.0.10 | Managed by Boot; the [system requirements](https://docs.spring.io/spring-boot/system-requirements.html) say "Spring Framework 7.0.10 or above is also required". |
| Spring Modulith | **2.1.2** | [api.spring.io](https://api.spring.io/projects/spring-modulith/generations) links 2.1.x to Boot 4.1.x; the [2.1.2 build file](https://github.com/spring-projects/spring-modulith/blob/2.1.2/pom.xml) sets `spring-boot.version` to 4.1.2. Released 2026-10-23. |
| Spring AI | **2.0.2** | [Getting started](https://docs.spring.io/spring-ai/reference/getting-started.html): "Spring AI 2.0.x supports Spring Boot 4.0.x and 4.1.x." The [2.0.2 build file](https://github.com/spring-projects/spring-ai/blob/v2.0.2/pom.xml) sets `spring-boot.version` 4.1.2 and `java.version` 17. Released 2026-10-23. |

Gaps and caveats:

- **Neither Modulith nor Spring AI is in Boot's dependency management.** The Boot 4.1.2 BOM has no entries for them, so import `spring-modulith-bom` and `spring-ai-bom` yourself. [start.spring.io](https://start.spring.io/metadata/client) does this for you and offers both for Boot 4.0 up to (not including) 4.3.
- **Spring AI 2.0 is young.** Its line started in June 2026 (api.spring.io). The 1.x lines only ever paired with Boot 3.4/3.5, so there was no GA Spring AI for Boot 4 during roughly the first seven months of Boot 4. That gap is closed.
- **Spring AI 2.0 broke a lot of 1.x API.** The [upgrade notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) list, among others: `ChatModel` no longer runs an internal tool loop, `FunctionCallback` replaced by `ToolCallback`, `ToolCallAdvisor` renamed `ToolCallingAdvisor`, `PromptChatMemoryAdvisor` removed, options made immutable, MCP classes moved to `org.springframework.ai.mcp.*`. Most tutorials and blog posts still describe 1.x, so trust only the 2.0 reference docs.
- **Spring AI 2.0.x free support ends 2027-07-31**; 2.1 (paired with Boot 4.2) is at RC2.
- **The Modulith compatibility table in its docs is stale.** The appendix at the 2.1.2 tag ([source](https://github.com/spring-projects/spring-modulith/blob/2.1.2/src/docs/antora/modules/ROOT/pages/appendix.adoc)) still ends at "2.0 (snapshot)". The pairing above rests on api.spring.io and the build file instead.
- The Modulith 2.1.2 release notes include a fix for `@ApplicationModuleTest` on `@Nested` classes "under Spring Boot 4.2" ([#1891](https://github.com/spring-projects/spring-modulith/issues/1891)), a sign that 2.1.x is being kept working on 4.2. Modulith 2.1 also enables JDBC event publication schema creation by default ([2.1.0 notes](https://github.com/spring-projects/spring-modulith/releases/tag/2.1.0)); decide whether Flyway or Modulith owns that table.
- Minor inconsistency: api.spring.io still lists Spring Framework 7.0.8 as current, while Maven Central has 7.0.10 and Boot 4.1.2 requires it. Harmless when Boot manages the version.

What Boot 4 itself changes for a new project ([4.0 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide), [4.1 release notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.1-Release-Notes)):

- Smaller, focused starters. `spring-boot-starter-web` became `spring-boot-starter-webmvc`; Flyway and Liquibase need `spring-boot-starter-flyway` / `spring-boot-starter-liquibase`; each technology has a matching `-test` starter (for example `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`).
- **Jackson 3 is the default** (packages `tools.jackson`, annotations stay in `com.fasterxml.jackson.annotation`).
- Jakarta EE 11 / Servlet 6.1, Tomcat 11, Hibernate 7.4, JUnit 6.0.3, JSpecify null annotations. Undertow is removed.
- Code samples written for Boot 3 often need these renames.

## 3. Java version

- [Boot 4.1.2 system requirements](https://docs.spring.io/spring-boot/system-requirements.html): "requires at least Java 17 and is compatible with versions up to and including Java 27."
- [Spring Framework versions wiki](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-Versions): range "JDK 17-25+" for 7.x, and "We recommend JDK 25 or higher for production use with Spring Framework 7.x." JDK 17, 21 and 25 are the fully supported LTS releases.
- [Adoptium support roadmap](https://adoptium.net/support/): Temurin 21 is available until "at least Dec 2029", Temurin 25 until "at least Sep 2031".
- Library floors found in published POMs: Spring AI 2.0.2 and POI 6.0.0 need Java 17; OpenPDF 3.0.5 and Flying Saucer 10.7.0 are built for Java 21; nothing on the list needs Java 25.

**Java 25 is recommended, not required.** Java 21 on the developer machine works with everything here. For a new, long-lived project the cleaner choice is to target Java 25 from the first commit: it is what Spring recommends for production, it has two more years of free builds, and switching later means retesting. It costs one JDK install (or a Gradle toolchain / Docker build that fetches it).

## 4. Library compatibility

"Managed" means the version is set by Boot 4.1.2's BOM ([POM](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom)), so you declare the dependency without a version. Latest versions are from Maven Central metadata.

| Need | Library | Version | Boot 4.1 status |
|---|---|---|---|
| Migrations | Flyway | **12.4.0** managed (latest upstream 13.12.1) | Supported through `spring-boot-starter-flyway`. PostgreSQL needs the separate `flyway-database-postgresql` module (also managed). Apache-2.0. |
| Migrations | Liquibase | 5.0.4 managed (also latest) | Supported through `spring-boot-starter-liquibase`. Licence is now FSL-1.1-ALv2, not a standard open-source licence. |
| Integration tests | Testcontainers | **2.0.5** managed (also latest, 2026-04-20) | Supported through `spring-boot-testcontainers`. |
| Database driver | PostgreSQL JDBC | **42.7.14** managed (also latest, 2026-10-07) | Supported. |
| Excel reading | Apache POI (`poi-ooxml`) | 6.0.0 (2026-10-19); previous 5.5.1 | Not managed, no Spring dependency. 6.0.0 needs Java 17+. |
| Excel reading | fastexcel-reader | 0.20.2 (2026-06-08) | Not managed, no Spring dependency. Built for Java 8+. |
| PDF | OpenPDF | 3.0.5 (2026-05-22) | Not managed, no Spring dependency. Built for Java 21. |
| PDF from HTML | Flying Saucer (`flying-saucer-pdf`) | 10.7.0 (2026-10-24) | Not managed. Built for Java 21, renders through OpenPDF. |
| PDF from HTML | openhtmltopdf (`io.github.openhtmltopdf`) | 1.1.110 (2026-11-07) | Not managed. Uses PDFBox 3.0.8. |
| PDF reports | JasperReports | 7.1.1 (2026-11-01) | Not managed. LGPL-3.0. |
| OpenAPI document | springdoc-openapi | **3.2.0** (2026-10-24) | Built on Boot 4.1.2. |
| TypeScript client | openapi-typescript 7.13.0 + openapi-fetch 0.17.0, or @hey-api/openapi-ts 0.99.0, or orval 8.47.0, or OpenAPI Generator 7.27.0 | see left | Independent of Boot: they read the OpenAPI JSON. |

Notes per item:

- **Flyway.** Keep the Boot-managed 12.4.0; do not override it to 13.x. Redgate's [PostgreSQL page](https://documentation.red-gate.com/fd/postgresql-database-277579325.html) lists PostgreSQL 18 as the newest verified version. Boot 4.0.9 manages a different major (11.14.1), so the Flyway major moves with Boot minors.
- **Liquibase.** The [LICENSE file](https://github.com/liquibase/liquibase/blob/master/LICENSE.txt) on the main branch is "Functional Source License, Version 1.1, ALv2 Future License", copyright 2025. For a repo that may go public and wants no licence questions, Flyway is the simpler pick.
- **Testcontainers 2.** The [2.0.0 release notes](https://github.com/testcontainers/testcontainers-java/releases/tag/2.0.0) say: JUnit 4 support removed; "All modules are now prefixed with `testcontainers-`"; container classes moved to `org.testcontainers.<module-name>`. So the dependency is `org.testcontainers:testcontainers-postgresql` and the class is `org.testcontainers.postgresql.PostgreSQLContainer`. The old `org.testcontainers:postgresql` artifact stopped at 1.21.4. Older examples will not compile unchanged.
- **PostgreSQL server.** [postgresql.org versions](https://www.postgresql.org/versions.json) lists 18 as the current major (supported until 2030-11-14) and 17 until 2029-11-08. PostgreSQL 14 reaches end of life today.
- **Excel.** Neither reader touches Spring, so Boot 4 is not a factor; only the Java floor and shared transitive libraries matter (both use commons-compress 1.28.0). [POI 6.0.0](https://poi.apache.org/) was announced 2026-10-22 with "POI requires Java 17 or newer since version 6.0.0"; it is three weeks old, and 5.5.1 is the conservative fallback. fastexcel-reader's parent POM declares POI 5.5.1 only for its own tests and benchmarks.
- **PDF.** None of the candidates touch Spring. The original `com.openhtmltopdf` coordinates were last published in 2021 and are dead; the maintained fork is [`io.github.openhtmltopdf`](https://github.com/openhtmltopdf/openhtmltopdf). JasperReports works but is a heavy reporting engine for what is one purchase order layout.
- **springdoc.** The [springdoc FAQ](https://springdoc.org/) matrix says Boot 4.1.x pairs with springdoc 3.1.x to 3.2.x and Boot 4.0.x with 3.0.x; the [3.2.0 POM](https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi/3.2.0/springdoc-openapi-3.2.0.pom) has `spring-boot-starter-parent` 4.1.2 as parent. **springdoc trails Boot minors**: Boot 4.1.0 shipped 2026-06-10 and springdoc 3.1.0 on 2026-08-01. Expect the same wait after Boot 4.2.0.
- **TypeScript client.** The npm tools need Node 22 or later (`@hey-api/openapi-ts` and `orval` declare `>=22.18.0`), which Node 24 satisfies. OpenAPI Generator is a Java tool (built for Java 11) with a Maven plugin, if generating from the Java build is preferred.

## 5. Oracle Cloud free ARM machines

Source: [Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm).

- Allowance: "the first 1,500 OCPU hours and 9,000 GB hours per month for free ... For Always Free tenancies, this is equivalent to 2 OCPUs and 12 GB of memory", as "one or two" instances.
- Storage: 200 GB of boot and block volume combined, five volume backups, home region only.
- Images offered for A1: Oracle Linux, Ubuntu, Oracle Linux Cloud Developer (needs 8 GB).
- **Idle reclamation**: an instance counts as idle if over 7 days the 95th-percentile CPU use is under 20%, network use is under 20% and (on A1) memory use is under 20%. A lightly used internal app could fall under this.
- "Out of host capacity" errors mean no free A1 capacity in the home region at that moment; Oracle says to retry or upgrade to a paid account (Always Free resources stay free after upgrading).

What it means for the stack:

- **JDK**: Temurin has Linux aarch64 builds of both 21 (`jdk-21.0.13+9`) and 25 (`jdk-25.0.5+8`), per the [Adoptium API](https://api.adoptium.net/v3/assets/latest/25/hotspot?architecture=aarch64&os=linux&image_type=jdk).
- **Docker images**: the official images all publish `linux/arm64` variants (checked on Docker Hub today): `postgres:18`, `postgres:17`, `postgres:18-alpine`, `eclipse-temurin:25-jre`, `eclipse-temurin:21-jre`, `eclipse-temurin:25-jre-alpine`, `amazoncorretto:25`, `node:24-alpine`, `nginx:stable-alpine`, `caddy:2`, `pgvector/pgvector:pg18`, and the Testcontainers `ryuk` helper.
- **Docker Engine** supports arm64 on Ubuntu 22.04, 24.04 and 26.04 ([Docker docs](https://docs.docker.com/engine/install/ubuntu/)).
- **The one real constraint: the application image must be built for `linux/arm64`.** An image built on an x86 laptop will not run on the VM. Either build on the VM, or build a multi-platform image (`docker buildx build --platform linux/arm64`), or build in CI. A plain Dockerfile on `eclipse-temurin:25-jre` is the low-risk route. Boot's `build-image` goal has an `imagePlatform` setting ([docs](https://docs.spring.io/spring-boot/maven-plugin/build-image.html)), but arm64 support of its default builder was not verified.
- All Java libraries on the list are pure JVM code, so the CPU architecture does not affect them.
- **Sizing**: 12 GB is still comfortable for one Boot app, PostgreSQL and a static React build, but plan for 2 OCPUs.

## 6. Maven or Gradle

Boot 4 does not push the choice either way. From the [system requirements](https://docs.spring.io/spring-boot/system-requirements.html): Maven "3.6.3 or later"; Gradle "8.x (8.14 or later) and 9.x". Small points:

- start.spring.io defaults to Gradle; both are first-class.
- Boot 4.1 with Maven: "You can no longer rely on passing `-DskipTests` ... to skip AOT processing of tests" (only matters with AOT/native builds).
- The springdoc Gradle plugin "needs Gradle 8.0 or later"; its Maven plugin needs Maven 3.6.3+.
- Modulith, Spring AI, Flyway and OpenAPI Generator all ship for both.

## Not verified

- **Industry adoption figures.** No first-party statistic exists. The only numbers found are secondary: an Axiom JDK survey of 300+ Java developers, mostly at large Russian companies ([ComNews, 2026-08-31](https://www.comnews.ru/content/247141/2026-08-31/2026-w36/1010/bolshinstvo-java-razrabotchikov-prodolzhayut-ispolzovat-spring-no-ne-sledyat-za-srokami-ego-obnovleniya)), reports 26% using Boot 4.x, 83.7% using 3.x and 21.2% using 2.x (multiple answers allowed), with 18.9% having started or finished a Boot 4 migration. The [OpenLogic 2026 report summary](https://www.openlogic.com/blog/java-ecosystem-trends) gives 52.05% on Boot 3.5 and no figure for 4.x. Treat these as rough signals.
- **Exact GA dates of Boot 4.0.0 and Spring AI 2.0.0.** api.spring.io only gives month-end placeholders (2025-11-30 and 2026-06-30).
- **Whether Boot 4.2.0 will ship on 2026-11-19**, and when Modulith 2.2, Spring AI 2.1 and a matching springdoc release reach GA.
- **Nothing was compiled or run.** Compatibility claims rest on published metadata and docs, not on a test build of this exact combination.
- **Paketo buildpack arm64 support** for Boot's default builder `paketobuildpacks/builder-noble-java-tiny`.
- **Which Liquibase release switched licence**, and the licences of OpenPDF, fastexcel, openhtmltopdf and Flying Saucer (GitHub's API could not classify them; openhtmltopdf's README says LGPL).
- **Java floors of JasperReports 7.1.1, Liquibase 5.0.4 and Testcontainers 2.0.5** (not stated in the POMs read).
- **openhtmltopdf**: the README changelog stops at 1.1.8 while Maven Central has 1.1.110, and the README says "requires at least Java 8"; the README may be out of date for current releases.
- **fastexcel-reader's file format coverage** (whether it reads old `.xls`) and whether either Excel reader copes with the real BigSeller exports. Needs a test with the sample files.
- **PDF rendering of non-Latin text** (Chinese supplier names) in any of the PDF libraries. Needs a small spike.
- **Flyway's upper bound for PostgreSQL** beyond "18 verified", and the developer machine's CPU architecture (assumed x86-64).

## Recommended starting versions

| Component | Version | How it is set |
|---|---|---|
| Spring Boot | 4.1.2 | Parent / plugin version |
| Java | 25 (21 also works) | Toolchain and Docker base image |
| Spring Framework | 7.0.10 | Managed by Boot |
| Spring Security | 7.1.2 | Managed by Boot |
| Spring Modulith | 2.1.2 | Import `spring-modulith-bom` |
| Spring AI | 2.0.2 | Import `spring-ai-bom` |
| Hibernate ORM | 7.4.13.Final | Managed by Boot |
| PostgreSQL server | 18 (`postgres:18`) | Docker image |
| PostgreSQL JDBC driver | 42.7.14 | Managed by Boot |
| Flyway | 12.4.0, plus `flyway-database-postgresql` | Managed by Boot |
| Testcontainers | 2.0.5 (`testcontainers-postgresql`) | Managed by Boot |
| JUnit | 6.0.3 | Managed by Boot |
| springdoc-openapi | 3.2.0 | Explicit version |
| TypeScript client generator | openapi-typescript 7.13.0 with openapi-fetch 0.17.0 (alternatives listed above) | npm, frontend |
| Excel reader | fastexcel-reader 0.20.2 or Apache POI 5.5.1 / 6.0.0 | Explicit version; decide with the sample files |
| PDF | Flying Saucer 10.7.0 or openhtmltopdf 1.1.110 (HTML template to PDF), or OpenPDF 3.0.5 directly | Explicit version; decide with a spike |
| Runtime image | `eclipse-temurin:25-jre`, built for `linux/arm64` | Dockerfile |
| Build tool | Maven 3.6.3+ or Gradle 8.14+/9.x | Either |

The Excel reader, PDF library and TypeScript generator rows are shortlists, not decisions: all options are compatible, and the choice depends on the import and purchase order tickets.

## Is Spring Boot 4 used in industry, and is its ecosystem consistent?

**Yes to both, with one habit to adopt.**

Spring Boot 4 has been generally available for about a year and is on its second minor release, with the third due next week. More telling than any survey: free support for Spring Boot 3 ended on 2026-06-30, so 4.x is now the only line that gets free security fixes, and start.spring.io no longer offers anything older than 4.0. Most existing company applications are still on 3.x (the one survey found suggests roughly a quarter of teams use 4.x somewhere), because migrating old code takes time. That is a reason not to migrate in a hurry; it is not a reason to start a new project on a line whose free support is already over.

The ecosystem is consistent. Every library this project needs has a release made for Boot 4.1: Spring's own projects ship on the same day as Boot, Boot manages the versions of Flyway, Testcontainers, Hibernate and the PostgreSQL driver, and springdoc publishes a matching line. The Excel and PDF libraries do not depend on Spring at all.

The habit: **third-party libraries follow each Boot minor by a few weeks** (springdoc took about seven weeks after Boot 4.1.0), and Spring AI took seven months to support Boot 4 at all. So do not upgrade to a new Boot minor on release day. Wait for the first patch release and check that Modulith, Spring AI and springdoc have matching versions. Also expect tutorials written for Boot 3, Spring AI 1.x and Testcontainers 1.x to need adjusting (starter names, Jackson 3 packages, Testcontainers artifact names).
