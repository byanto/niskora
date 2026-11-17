# Build order

The nine steps in which niskora v1 is built. Each step has a goal, what it depends on and how to tell it is done.

Read [README.md](README.md) first. This file orders the work and adds no behaviour: every rule a step builds is in the area file it names. It does not cut build tickets. That happens after the map, and one step becomes several tickets.

## Why this order

- Deployment is first, so that every later step is used on real data the day it merges.
- Location analysis needs only orders, so it comes before the purchasing chain.
- The agent is last, because its tools sit on every other module.
- The end of step 3 is the first slice the operator uses daily.

## The steps at a glance

| Step | Builds | Backend modules | Depends on |
| --- | --- | --- | --- |
| 1 | Walking skeleton | `common`, `identity` | nothing |
| 2 | Merchant SKU list import and the catalogue screens | `catalogue`, `imports` | 1 |
| 3 | Inventory snapshot and order imports, packages, Home | `orders`, `stock`, `imports` | 2 |
| 4 | Stock movement import and the history load | `stock`, `imports` | 3 |
| 5 | Customer location analysis | `locations` | 3 |
| 6 | Suppliers, supply sources and the supply source sheet | `purchasing` | 2 |
| 7 | Restock advice | `restock` | 4, 6 |
| 8 | Purchase orders, the three documents and the stock-in match | `purchasing` | 4, 6, 7 |
| 9 | The agent | `agent` | 1 to 8 |

The steps are built in number order. The "depends on" column says what a step truly needs, which matters if a step is ever delayed: step 5 and step 6 do not need step 4, and step 6 does not need step 3.

The module dependencies are in [engineering.md](engineering.md). A module is created in the step that first needs it and not before: no step leaves an empty module behind.

## Step 1: Walking skeleton

**Goal.** An empty niskora that a user can log in to, deployed to production, built and guarded the way every later step will be.

**Depends on.** Nothing.

**Builds.**

- The repository scaffold, on the newest Spring Boot 4 release that Spring Modulith and Spring AI both support that day, with the exact versions written into [engineering.md](engineering.md) (ENG-2): `backend/` on Maven, `frontend/` on Vite with pnpm, and the Compose stack, as in [engineering.md](engineering.md).
- The merge gate in CI with every check, the protected `main` branch, the weekly dependency updates and the first migration.
- The `common` and `identity` modules: login, the session, the Users screen (adding, resetting a password, disabling and enabling) and changing your own password, as in [platform.md](platform.md).
- The frontend shell with its sidebar, the design tokens, `docs/design.md`, `frontend/CLAUDE.md`, the fixed loading, empty and error states, and the translation setup, as in [frontend.md](frontend.md).
- Production on the OCI machine: the Compose stack, the Cloudflare Tunnel, the secrets file, the image build on merge and the manual deploy workflow.
- Backups: the nightly dump to Object Storage, the weekly copy to Cloudflare R2 and the monthly restore test.
- Monitoring: the checks on Healthchecks.io with their alerts to Discord, the logs, and the runbook with one section per check.

**Done when.**

- The owner and the operator each log in at `niskora.budiyanto.com` with their own account and see the empty shell.
- A pull request cannot merge with a failing check, and a merge to `main` produces `linux/arm64` images.
- The owner deploys a tag with the manual workflow and rolls back to the previous tag.
- A nightly dump is in Object Storage, a copy is in R2, and one restore on the second machine has passed.
- Stopping the stack on purpose raises an alert in Discord within 10 minutes.
- One Playwright journey (log in, see Home's empty state, log out) passes against the Compose stack.

_Area files: [platform.md](platform.md), [engineering.md](engineering.md), [frontend.md](frontend.md)._

## Step 2: Merchant SKU list import and the catalogue screens

**Goal.** The catalogue is in niskora: every [Merchant SKU](../../GLOSSARY.md#catalogue), [Combination SKU](../../GLOSSARY.md#catalogue), [SPU](../../GLOSSARY.md#catalogue) and [company](../../GLOSSARY.md#catalogue), kept current by uploading BigSeller's Merchant SKU list.

**Depends on.** Step 1.

**Builds.**

- The `imports` module: the upload with its drop zone, file type detection, the stored original file, the [import](../../GLOSSARY.md#imports) record and its report, background processing, the repeated-file check and staleness.
- The Merchant SKU list import with its Combination SKU sheet, into the `catalogue` module: SPUs derived from the code, companies from the prefix table, retiring, and the "Merchant SKUs added" event.
- Photos copied from BigSeller's Image URL.
- Screens: Imports, Import report, Catalogue with its three tabs, SPU page and Merchant SKU page (catalogue fields only), and Settings with the read-only prefix table.
- The synthetic export files for this data set.

**Done when.**

- The operator uploads the real Merchant SKU list on production and the catalogue screens show every Merchant SKU with its SPU, company and photo.
- A second upload of a newer list reports what was added, changed and [not seen](../../GLOSSARY.md#imports).
- A structurally wrong file is rejected whole, with the reason in its report.
- The data freshness chip shows the Merchant SKU list as fresh, and as [stale](../../GLOSSARY.md#imports) once its allowed age has passed.

_Area files: [foundation.md](foundation.md), [frontend.md](frontend.md)._

## Step 3: Inventory snapshot and order imports, packages and Home

**Goal.** The operator's daily routine exists: upload the day's exports, see what changed, and see on Home what needs attention. This is the first slice used daily.

**Depends on.** Step 2.

**Builds.**

- The inventory list import into the `stock` module as an [inventory snapshot](../../GLOSSARY.md#stock).
- A comparison of BigSeller's three per-line order layouts on a sample, to learn whether a file in the wrong layout can be rejected (FND-95).
- The order import into the `orders` module: packages replaced as a set, [component lines](../../GLOSSARY.md#orders), [held packages](../../GLOSSARY.md#imports) and their release on "Merchant SKUs added", [hub packages](../../GLOSSARY.md#orders), [unmapped lines](../../GLOSSARY.md#orders), [abandoned packages](../../GLOSSARY.md#orders), [demand](../../GLOSSARY.md#orders), and the buyer personal data in its own schema.
- Screens: Packages, Package page, and Home with data freshness, "needs attention" and yesterday's counts. The SPU page and the Merchant SKU page gain the stock figures and the demand chart.
- The search box in the shell.
- The monitoring check for an orders import within 3 days.

**Done when.**

- The operator uploads the daily order and inventory exports on production each morning and reads the result on Home, with no help from the owner.
- A re-uploaded order export changes only the packages that changed in BigSeller, and nothing is ever deleted because it is absent.
- A package with an unknown Merchant SKU is held and is released after a newer Merchant SKU list is imported.
- Buyer fields show on the Package page and nowhere else, and the module boundary test for personal data passes.
- A day without uploads shows as stale on Home and in the chip after 36 hours.

_Area files: [foundation.md](foundation.md), [frontend.md](frontend.md), [platform.md](platform.md)._

## Step 4: Stock movement import and the history load

**Goal.** niskora holds the full past: every [stock movement](../../GLOSSARY.md#stock) and every order BigSeller still has, so that later steps can measure sales rates and [stockout days](../../GLOSSARY.md#restock).

**Depends on.** Step 3.

**Builds.**

- The stock movement import, which the operator uploads daily from the day it is deployed, with the history load run right after: the merge on a natural key, the skipped shelf moves, unseen movement types stored and flagged, and [stock history gaps](../../GLOSSARY.md#stock).
- The history load: long-range order and stock movement exports accepted through the same upload, marked so that their files are kept forever.
- The stock movements on the Merchant SKU page, and the findings of this import in the report and under "needs attention".

**Done when.**

- The history load has been run on production: orders back to BigSeller's limit and stock movements back to their first day, with every movement chain linking across files.
- The daily 7-day stock movement export merges into that history without duplicates, and a missed day is healed by the next upload.
- The performance test on ten years of synthetic data passes for the queries built so far.
- All four data sets show on Home with their freshness.

_Area files: [foundation.md](foundation.md), [platform.md](platform.md), [engineering.md](engineering.md)._

## Step 5: Customer location analysis

**Goal.** The owner sees where packages go, by province, city and district, and can aim place-targeted ads with it.

**Depends on.** Step 3. It is richer after step 4, because the history load gives it longer periods.

**Builds.**

- The `locations` module: the official list of [places](../../GLOSSARY.md#locations), [place aliases](../../GLOSSARY.md#locations), matching of [destinations](../../GLOSSARY.md#orders) and the analysis in both directions.
- The province boundary file and the map.
- Screens: Location analysis with its table, map and CSV, and Place aliases. The Package page gains the matched place, and the SPU page its link to the analysis.
- [Unmatched destinations](../../GLOSSARY.md#locations) under "needs attention", and the link from the order import report to the Place aliases screen. An import report does not list them.
- The choice of the file the place list is taken from, checked against the government decree.

**Done when.**

- The owner picks an SPU on production and sees its packages, units and goods value per province, city and district against the preceding period, in the table and on the map.
- Correcting one unmatched spelling moves every package with that spelling to its place.
- Lazada packages show as unknown and never as a guessed place.
- The CSV holds the same figures as the screen and no buyer field.
- Every island of 100 km² or more is drawn on the map, or is listed in [locations.md](locations.md) with the reason it is missing (LOC-43).

_Area files: [locations.md](locations.md), [frontend.md](frontend.md)._

## Step 6: Suppliers, supply sources and the supply source sheet

**Goal.** niskora knows who each [Single SKU](../../GLOSSARY.md#catalogue) is bought from, through which [buying channel](../../GLOSSARY.md#purchasing), at what price and with what [lead time](../../GLOSSARY.md#purchasing).

**Depends on.** Step 2.

**Builds.**

- The `purchasing` module, first part: [suppliers](../../GLOSSARY.md#purchasing) typed by hand, [supply sources](../../GLOSSARY.md#purchasing), the [preferred supply source](../../GLOSSARY.md#purchasing), carton data, and inheriting supply sources when an import adds a Single SKU.
- The [supply source sheet](../../GLOSSARY.md#purchasing): download, upload, and the rejection of a whole file with every faulty row listed.
- Screens: Suppliers, Supplier page, the supply sources on the Merchant SKU page, and the default lead time on Settings.

**Done when.**

- The owner has entered the real suppliers on production and loaded supply sources for them through the sheet.
- A sheet with one faulty row changes nothing and names that row and its reason.
- An upload never deletes a supply source and an empty cell never clears a stored value.
- Every change to a supply source shows in its audit trail with who made it.

_Area files: [purchasing.md](purchasing.md), [frontend.md](frontend.md)._

## Step 7: Restock advice

**Goal.** The operator sees what to reorder, from whom and how much, with the working shown for every line.

**Depends on.** Step 4 (sales rates need the order history, and stockout days need the stock movements) and step 6 (lead times and supply sources).

**Builds.**

- The `restock` module: the stored sales rates, rebuilt on "import completed", on a campaign day change and on a new calculation version, and the [restock advice](../../GLOSSARY.md#restock) worked out live from them.
- Screens: Restock advice with its supplier blocks and the "No supplier" block, and Campaign days. The SPU page and the Merchant SKU page gain the sales rate, days left, the restock level and the working. Home and the sidebar gain the restock counts. Settings gains the safety days and the default order cycle.

**Done when.**

- Restock advice on production shows every advised Single SKU grouped by supplier then SPU, and the owner has checked the working of a sample of lines by hand against BigSeller.
- Marking a campaign day changes the sales rates that include it.
- A Single SKU with no supply source appears in the "No supplier" block, and assigning a supplier there moves it to that supplier's block.
- With a stale data set the screen says so.
- The performance test passes for the advice queries.

_Area files: [restock.md](restock.md), [frontend.md](frontend.md)._

## Step 8: Purchase orders, the three documents and the stock-in match

**Goal.** Restock advice becomes a [purchase order](../../GLOSSARY.md#purchasing) that is placed, documented and followed until the goods are received, without typing a received quantity into niskora.

**Depends on.** Step 7 (a draft is made from advice), step 6, and step 4 (received quantities come from [Purchase Stock-In](../../GLOSSARY.md#stock) movements).

**Builds.**

- The `purchasing` module, second part: purchase orders and their lines, the statuses, the audit trail, [shortfalls](../../GLOSSARY.md#purchasing) and their outcomes, [resend lines](../../GLOSSARY.md#purchasing), and [on order](../../GLOSSARY.md#purchasing) feeding back into restock advice.
- The three documents: the [Daftar Pesanan](../../GLOSSARY.md#purchasing), the forwarder invoice and the BigSeller import file.
- The stock-in match.
- Screens: Purchase orders, Purchase order page, Match stock-ins and Forwarders. Restock advice gains "Create draft purchase order". Home and the sidebar gain the purchase order counts and the unmatched stock-in count. Settings gains the trading name.

**Done when.**

- The owner creates a draft from restock advice on production, places it, and sends the Daftar Pesanan to a real supplier.
- The BigSeller import file made by niskora is accepted by BigSeller, including one with an empty Unit Price (PUR-93).
- After the goods are stocked in through BigSeller and the next stock movement import, the operator confirms the match and the lines show their received quantities.
- A purchase order through 1688 produces the forwarder invoice with the marking code.
- Two users changing the same purchase order at once get the conflict message and lose nothing they typed.

_Area files: [purchasing.md](purchasing.md), [restock.md](restock.md), [frontend.md](frontend.md)._

## Step 9: The agent

**Goal.** A user asks a question in plain language and gets an answer from niskora's own data, with the tools it used and how fresh the data is.

**Depends on.** Steps 1 to 8, because each of its tools calls the public query interface of another module.

**Builds.**

- The `agent` module: the fixed read-only tools, the AI provider behind configuration, saved private [conversations](../../GLOSSARY.md#agent), streamed answers, the daily limit and the fallback model.
- The Ask niskora screen.
- The tests that keep buyer data out, the scripted model for CI, and the hand-run set of real questions.

**Done when.**

- The owner asks the hand-run set of questions on production, and every figure in every answer matches the screen the answer links to.
- No answer contains a buyer field, and the module boundary tests pass.
- The agent changes nothing: it has no tool that writes.
- Reaching the provider's limit shows a clear message in the app and on Home.
- The whole of v1 has one Playwright journey per capability, all passing.

_Area files: [agent.md](agent.md), [frontend.md](frontend.md)._

## After step 9

v1 is complete. Pricing is the first feature after it, and its model is in [later/pricing.md](later/pricing.md).

_Sources: [Assemble the v1 spec and build order](https://github.com/byanto/niskora/issues/29), [Bounded contexts and module layout](https://github.com/byanto/niskora/issues/6), [Deployment, login and roles](https://github.com/byanto/niskora/issues/12), [Monitoring and alerting](https://github.com/byanto/niskora/issues/27), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._
