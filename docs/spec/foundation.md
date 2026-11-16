# Foundation: imports, catalogue, orders and stock

How BigSeller's exports become the [catalogue](../../GLOSSARY.md#catalogue), the order history and the stock history that every other part of niskora reads. Rule ids start with `FND`.

Read [README.md](README.md) first. Terms are defined in [`GLOSSARY.md`](../../GLOSSARY.md) and are not redefined here.

## 1. Data sets and rhythm

- **FND-1** niskora imports four data sets, each from one BigSeller [export](../../GLOSSARY.md#imports):

  | Data set | Export | How often |
  | --- | --- | --- |
  | Orders | the 90 Days preset by Order Time, one row per line by Merchant SKU, all statuses | daily |
  | Inventory list | all Merchant SKUs | daily |
  | Stock movements | the last 7 days, All Types | daily |
  | Merchant SKU list, with its Combination SKU sheet | all Merchant SKUs | on change, at least weekly |

- **FND-2** The daily order export covers 90 days because a package keeps changing status long after its Order Time and BigSeller has no filter on update time. A shorter range would miss later changes.
- **FND-3** The daily stock movement export covers 7 days, so a missed day is healed by the next upload. Overlap is merged (FND-67).
- **FND-4** All dates and times in the exports are Jakarta time (WIB). A [business day](../../GLOSSARY.md#imports) is midnight to midnight WIB.
- **FND-5** Nothing else is imported: not BigSeller's purchase orders, supplier list, customer list, SKU mapping, purchase suggestions, Supply Source Management data, Reference Cost Price, SPU field or shelf export, not a settlement or fee export, and not the JSON captures.

_Sources: [Import design for BigSeller exports](https://github.com/byanto/niskora/issues/7), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17), [Domain model and glossary](https://github.com/byanto/niskora/issues/5), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 2. Upload and the import

- **FND-6** One upload screen takes every export type. It accepts `.zip`, `.xlsx` and `.csv`, recognised by content and not by file name.
- **FND-7** The export type is detected from the header row. The user never picks it.
- **FND-8** A zip is unpacked on the server and is one [import](../../GLOSSARY.md#imports). A zip that holds an unrecognised file, files of more than one export type, or another zip is rejected whole.
- **FND-9** An import is accepted or rejected as a whole. Applying it is one database transaction.
- **FND-10** A file that is structurally wrong (wrong columns, wrong layout, not a BigSeller export) is rejected. Nothing changes, and the message says what is wrong.
- **FND-11** The original upload is stored as received in Object Storage before it is processed. If that copy fails, the import fails as a whole.
- **FND-12** niskora takes a SHA-256 of every uploaded file. A file identical to one already imported successfully is not processed again: the answer points at that earlier import.
- **FND-13** An export that BigSeller delivers as several files is uploaded as one zip, so that it is one import. This matters most for the Merchant SKU list, which is a full replacement (FND-16).
- **FND-14** Every import records who uploaded it and when.
- **FND-15** The upload is answered as soon as the file is stored, with the import marked "processing". The import runs in the background, and the user can leave the page.
- **FND-94** BigSeller ends the name of an export file with the moment it was made, as 17 digits: year, month, day, hour, minute, second and milliseconds, in Jakarta time. `Order-Goods-all20261116110200674.zip` was exported on 16 November 2026 at 11:02:00. niskora reads this [export time](../../GLOSSARY.md#imports) from the name of the uploaded file and records it on the import. A file that was renamed, or a long-range export that BigSeller names by its date range, has no export time.

_Sources: [Import design for BigSeller exports](https://github.com/byanto/niskora/issues/7), [Bounded contexts and module layout](https://github.com/byanto/niskora/issues/6), [API conventions](https://github.com/byanto/niskora/issues/30), [Monitoring and alerting](https://github.com/byanto/niskora/issues/27), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._

## 3. Catalogue

### The Merchant SKU list import

- **FND-16** The Merchant SKU list is a full replacement. Every [Merchant SKU](../../GLOSSARY.md#catalogue) in it is added or updated. A Merchant SKU that niskora knows and the list no longer contains becomes a [Retired Merchant SKU](../../GLOSSARY.md#catalogue) and is kept with all its history. An import never deletes anything.
- **FND-17** A Merchant SKU list import that would retire more than 2% of the Merchant SKUs stops before it changes anything and asks a user to confirm.
- **FND-18** The Merchant SKU code is the key that joins every export. Cells are trimmed, and a code is otherwise kept and compared exactly as exported. A code is never changed in place: BigSeller does not allow it, and other modules hold copies of it ([ADR 0003](../adr/0003-one-database-schema-per-module.md)).
- **FND-19** A Merchant SKU is either a [Single SKU](../../GLOSSARY.md#catalogue) or a [Combination SKU](../../GLOSSARY.md#catalogue). A Combination SKU's [components](../../GLOSSARY.md#catalogue) and their quantities are read from the second sheet of the Merchant SKU export, never from its code. Every component is a Single SKU.

### SPUs and companies

- **FND-20** niskora derives the [SPU](../../GLOSSARY.md#catalogue) of every Single SKU from its code: the part before the first `-`. A code that does not follow the convention (a prefix of letters, four digits, then an optional `-` and more) is an SPU of one. A user can correct a Merchant SKU's SPU by hand, and an import never undoes that. A Combination SKU belongs to no SPU by its code.
- **FND-21** Every SPU belongs to one [company](../../GLOSSARY.md#catalogue), read from the code prefix through a fixed prefix table: `KRN`, `KC` and `BDL` are Kirana; `LM` is Lumio; `SJ`, `SK`, `SP`, `SA`, `SN`, `SM`, `ST` and `SNJ` are Senja. The table is seeded by a migration and is read-only in Setup. A user can correct an SPU's company by hand. An SPU whose prefix gives no company shows as "no company" under "needs attention".
- **FND-22** A Combination SKU takes its company from its own code prefix through the same table. Only when its prefix is not in the table does it take the company of its components, and when those belong to different companies it has none. A Combination SKU with no company is not listed under "needs attention".
- **FND-23** A user can mark a Merchant SKU, or a whole SPU at once, as a [Discontinued Merchant SKU](../../GLOSSARY.md#catalogue). The mark is niskora's own and no import changes it. What it does to restock advice is in [restock.md](restock.md).
- **FND-24** [Listings](../../GLOSSARY.md#catalogue) are not held in v1.

### Photos

- **FND-25** The catalogue keeps one photo per Merchant SKU, copied from the Image URL in the Merchant SKU export, scaled to at most 1,000 px on the long side and kept in Object Storage.
- **FND-26** Photos are fetched in the background after a Merchant SKU list import, and again only when the URL changes. A failed download never fails the import and is tried again after the next one.
- **FND-27** A Merchant SKU with no photo yet, or with no Image URL, shows a neutral placeholder on screens and documents.
- **FND-28** An SPU's photo is that of its first Merchant SKU unless a user picks another. There is no photo upload by hand in v1.

_Sources: [Import design for BigSeller exports](https://github.com/byanto/niskora/issues/7), [Provide overlapping order re-exports and the missing export types](https://github.com/byanto/niskora/issues/16), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17), [Domain model and glossary](https://github.com/byanto/niskora/issues/5), [Restock calculation rules](https://github.com/byanto/niskora/issues/8), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._

## 4. Orders

### Reading an order export

- **FND-29** The order export is the per-line layout by Merchant SKU: one row per line, with the package's own values repeated on every row. Files are CSV with a byte order mark and cells that span lines, most numbers are stored as text, and `--` stands for an empty value. Cells are trimmed.
- **FND-95** BigSeller also offers the per-line order export by store SKU and by Single SKU, and the three files look alike unless a Combination SKU is present. The upload screen names the layout to pick, "by Merchant SKU", beside the drop zone. Whether a file gives its layout away is found out at the start of the build step for the order import, by comparing the three layouts on a sample. If it does, a file in another layout is rejected whole with a message that names the right layout. If it does not, the screen text is the only guard, and this rule is corrected to say so.
- **FND-30** The [package](../../GLOSSARY.md#orders), identified by its Package No, is the unit of import. The export has no line id, so a package's rows are always taken together. A package's rows may sit in two files of one zip.
- **FND-31** Rows of one package that name the same Merchant SKU are summed into one [package line](../../GLOSSARY.md#orders). Lazada exports one row per unit, and two listings can map to one Merchant SKU.
- **FND-32** A package always holds the values of the newest export that contains it. An older export uploaded later never overwrites it. Newer means a later export time (FND-94). For a file with no export time, the latest Order Time in the file stands in for it. A later upload breaks a tie.
- **FND-33** A package in the file replaces the stored package as a set: its lines, status and every other value. This holds whatever its status, [final](../../GLOSSARY.md#orders) or not. The Image Link is ignored when deciding whether a package changed.
- **FND-34** An import never deletes or cancels a package because it is absent. The range an export covers is read from the file itself, from its earliest to its latest Order Time. A stored package inside that range that the file does not contain is kept unchanged and reported as [not seen](../../GLOSSARY.md#imports). Only an explicit status cancels a package.
- **FND-35** Every status is stored as exported. The final statuses are Completed, Canceled and Return & Refund. A status niskora has never seen is stored, treated as not final, and flagged in the import report.
- **FND-36** A final status promises only that the package's lines no longer change. Other values can still change, and a Return & Refund package can go back to Completed.

### Stores and orders

- **FND-37** A [store](../../GLOSSARY.md#orders) is created the first time an order export names it, keyed by its Store Nickname, with its [marketplace](../../GLOSSARY.md#orders) and marketplace store name as exported. Marketplace values are stored exactly as exported: Shopee, Lazada, TikTok, Blibli, Manual.
- **FND-38** A user can set a store's company (at most one, or none) and an optional Shop ID. Any store may sell any company's Merchant SKUs, and niskora never flags it.
- **FND-39** An [order](../../GLOSSARY.md#orders) is only the Order No that groups packages. Nothing is keyed on it. The Order No is a value on the package, so a package can move to another order when a newer export says so. Order totals are stored as exported on each package and are never summed across packages.

### Lines

- **FND-40** A package line that names a Combination SKU is kept, and one [component line](../../GLOSSARY.md#orders) per component is created beside it from the current definition. Component lines are rebuilt each time the package is replaced, except that a package already stored in a final status keeps the component lines it has.
- **FND-41** A line whose Merchant SKU code the catalogue does not know, in a package that is in a final status, becomes an [unmapped line](../../GLOSSARY.md#orders): it keeps the exported code, title, quantity and price, has no Merchant SKU and belongs to no SPU. A line with an empty Merchant SKU is treated the same. No Retired Merchant SKU is ever created from an order line.
- **FND-42** A package that is not in a final status and has a line with an unknown code becomes a [held package](../../GLOSSARY.md#imports): it stays out of the order history and is listed under "needs attention". It is released in one of two ways:
  1. The catalogue learns the code. The package is then imported automatically, shortly after the Merchant SKU list import and not inside it. A failed release is retried and never undoes that import.
  2. A Merchant SKU list newer than the package's order export has been imported and the code is still unknown. The line becomes an unmapped line, the package is stored, and it stays under "needs attention" so that someone can map it in BigSeller. A later export that carries the real Merchant SKU replaces the package as usual.
- **FND-43** A package with a value that cannot be read (a quantity that is not a number, a missing Order Time) is left out of the import, counted in the import report as unreadable, and listed under "needs attention". The stored package, if there is one, stays as it was. The rest of the file is imported.

### Packages that are not customer orders

- **FND-44** A Shopee package in which no line has a Price is a [hub package](../../GLOSSARY.md#orders), whatever its status. It is stored and labelled as one.
- **FND-45** A hub package is never demand and never in location analysis, whether it is not yet matched, matched or canceled.
- **FND-46** A package with a Price on some lines only is stored as a normal package and flagged in the import report as an unseen shape.
- **FND-47** When Shopee matches a hub package to a buyer's order, the hub package takes the buyer's Order No. The order then shows two packages, and the hub one is labelled.
- **FND-48** Units sitting at a Shopee hub are ignored in v1. [Available](../../GLOSSARY.md#stock) stays exactly BigSeller's figure, and the unit becomes demand on the day the buyer orders, through the buyer's own package.
- **FND-49** A package is an [abandoned package](../../GLOSSARY.md#orders) when it is not in a final status, its Stock-Out value is `No`, and its Order Time is more than 90 days before the latest Order Time in the newest order export. It is listed under "needs attention" so that it can be canceled in BigSeller.

### Demand

- **FND-50** Only the `orders` module computes [demand](../../GLOSSARY.md#orders). Every screen, restock advice and the agent use its figure.
- **FND-51** Demand is the units of a Single SKU on the package lines and component lines of every package that is not Canceled, not abandoned and not a hub package, counted on the business day of the package's Order Time.
- **FND-52** A Return & Refund package counts. So does a package that shipped and never reached a final status, and an unpaid package until it is canceled.
- **FND-53** A Combination SKU's own line, an unmapped line, a held package and a stock movement are never demand.
- **FND-54** Demand can be asked per Single SKU per business day, and narrowed to a store or a company.

### Buyers and destinations

- **FND-55** v1 keeps no customer record. A [buyer](../../GLOSSARY.md#orders) exists only as fields on one package: name, phone, address, post code, Shopee username and the Customer Code of a manual order. Every such field the export does not mask is stored.
- **FND-56** Buyer fields sit in their own schema, keyed by Package No, behind their own interface, and are replaced with the package on every import ([ADR 0003](../adr/0003-one-database-schema-per-module.md)). They show only on a single package's own page and never in an analysis, a download or an agent answer.
- **FND-57** A package's [destination](../../GLOSSARY.md#orders) (province, city, district) is stored as text exactly as exported. A masked or empty destination is unknown. niskora never sets a destination on a single package by hand. Mapping the text to places belongs to [locations.md](locations.md).

_Sources: [Shape of the BigSeller exports, JSON captures and owner spreadsheets](https://github.com/byanto/niskora/issues/4), [Provide overlapping order re-exports and the missing export types](https://github.com/byanto/niskora/issues/16), [Import design for BigSeller exports](https://github.com/byanto/niskora/issues/7), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17), [Domain model and glossary](https://github.com/byanto/niskora/issues/5), [Bounded contexts and module layout](https://github.com/byanto/niskora/issues/6), [Restock calculation rules](https://github.com/byanto/niskora/issues/8), [Customer location analysis](https://github.com/byanto/niskora/issues/10), [Export the full order and stock movement history](https://github.com/byanto/niskora/issues/19), [Import rule changes from the full history](https://github.com/byanto/niskora/issues/23), [Re-export the same order date range a week later](https://github.com/byanto/niskora/issues/18), [Hub package and final status rules after the second order export](https://github.com/byanto/niskora/issues/24), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._

## 5. Stock

### Inventory snapshots

- **FND-58** Each inventory list import is an [inventory snapshot](../../GLOSSARY.md#stock): one row per Merchant SKU for the business day of the file's export time (FND-94). A file with no export time takes the business day of its upload, and the import report says which of the two was used. Every snapshot is kept. A second import for the same business day replaces that day's snapshot.
- **FND-59** A snapshot row holds [on hand](../../GLOSSARY.md#stock), [allocated](../../GLOSSARY.md#stock), available, [on the way](../../GLOSSARY.md#stock) and the [Weighted Average Cost](../../GLOSSARY.md#cost), all as BigSeller reports them. Promo Reserved and defective stock are stored and used by nothing in v1.
- **FND-60** The snapshot is the only source of allocated and on the way.
- **FND-61** There is one stock figure per Single SKU. The warehouse name in an export is stored as a plain label and is never validated.
- **FND-62** A Merchant SKU that niskora knows and an inventory list does not contain has no row in that snapshot and is reported as not seen. It is not retired. An inventory list import that would leave more than 2% of the Merchant SKUs not seen stops before it changes anything and asks a user to confirm.
- **FND-96** An inventory snapshot is not compared with the end of the movement chain in v1. The two exports are made minutes apart while orders keep arriving, so they rarely agree to the unit. The chain check (FND-74) is the only check on the stock history.

### Stock movements

- **FND-63** A [stock movement](../../GLOSSARY.md#stock) is stored with its type as exported, its time, its document number, its Package No, the stock before and after, the weighted cost before and after and, on a [Purchase Stock-In](../../GLOSSARY.md#stock), the stock-in price.
- **FND-64** The known types are sales stock-out, Purchase Stock-In, Return Stock-In, manual stock-in, manual stock-out, inventory gain, inventory loss and Canceled Order Stock-In. Only Purchase Stock-In has a meaning niskora acts on, described in [purchasing.md](purchasing.md).
- **FND-65** A type niskora has never seen is stored as exported, kept in the chain check and flagged in the import report as a new movement type. The file is not rejected.
- **FND-66** Moves between shelves (`Move SKU Onto Shelf`, `Move SKU Off Shelf`, `Move - Click to Move`) are not stock movements. They are skipped, left out of the chain check and counted in the import report as shelf moves skipped. The archived upload still holds them.
- **FND-67** The export has no movement id. Two rows are the same movement when they agree on Merchant SKU code, time, document number, type, Package No, stock before and movement quantity. Overlapping uploads are merged on that key, so a movement is stored once.
- **FND-68** When an uploaded row matches a stored movement, its other values replace the stored ones, except that an empty cell never clears a filled one. The result is the same whatever order the files are uploaded in.
- **FND-69** A stock movement whose Merchant SKU code the catalogue does not know creates a Retired Merchant SKU from the row's code and title, so the stock history stays complete.
- **FND-70** A movement for a Combination SKU's sale arrives already split: the component is the Merchant SKU of the row. The Combination SKU named beside it is stored.
- **FND-71** A stock movement is never demand. Movements supply the stock history, received quantities, returned quantities, adjustments and the cost history.
- **FND-72** Canceled Order Stock-In is a normal change to on hand and is not a return. A returned quantity means Return Stock-In only.
- **FND-73** The shelf code and the unit on a movement row are ignored.

### The chain check

- **FND-74** For each Single SKU the movements form a chain: every movement's stock before plus its quantity equals its stock after, and equals the stock before of the next one. After every stock movement import the chain is checked. A break is a [stock history gap](../../GLOSSARY.md#stock), reported with the dates to export again.
- **FND-75** A day with no movements is not a gap. A real second warehouse would show up as broken chains.

### Stockout days

- **FND-76** Only the `stock` module answers which business days a Single SKU began with no on hand. It works them out on request from the movement chain, for one Single SKU and a date range, and stores nothing, so a gap filled by a later import corrects the past by itself.
- **FND-77** Inside a stock history gap, and before the stock history starts, on hand is unknown and the answer says so. Inside the history, the days before a Single SKU's first movement use that movement's stock before.

_Sources: [Import design for BigSeller exports](https://github.com/byanto/niskora/issues/7), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17), [Domain model and glossary](https://github.com/byanto/niskora/issues/5), [Export the full order and stock movement history](https://github.com/byanto/niskora/issues/19), [Import rule changes from the full history](https://github.com/byanto/niskora/issues/23), [Reporting and read model approach](https://github.com/byanto/niskora/issues/15), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._

## 6. The import report

- **FND-78** Every import has one report of what it changed. It shows the export time read from the file name, or that there was none.
- **FND-79** An order import reports the packages that are new, changed, unchanged, held, unreadable and not seen, and the unseen statuses and shapes it met. It does not list destinations that match no place: those live on the correction page ([locations.md](locations.md), LOC-27), which the report links to.
- **FND-80** A stock movement import reports the movements that are new, changed and unchanged, the shelf moves skipped, the new movement types, the Retired Merchant SKUs it created and the stock history gaps it found.
- **FND-81** A Merchant SKU list import reports the Merchant SKUs added, changed and retired. An inventory list import reports the rows taken, the Merchant SKUs not seen, and the business day its snapshot was given (FND-58).
- **FND-82** A package that moved to another order counts as changed.

_Sources: [Import design for BigSeller exports](https://github.com/byanto/niskora/issues/7), [Customer location analysis](https://github.com/byanto/niskora/issues/10), [Import rule changes from the full history](https://github.com/byanto/niskora/issues/23), [Hub package and final status rules after the second order export](https://github.com/byanto/niskora/issues/24), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._

## 7. Freshness

- **FND-83** Orders, the inventory list and stock movements are [stale](../../GLOSSARY.md#imports) 36 hours after their last successful import. The Merchant SKU list is stale after 8 days.
- **FND-84** A stale data set shows a banner on every screen that depends on it, and the agent states the age of the data in its answers. Nothing is blocked.
- **FND-85** Only the `imports` module answers whether a data set is stale. Every other module asks it.
- **FND-86** Restock advice warns while any package is held.
- **FND-87** The foundation puts these under "needs attention": held packages, packages stored with an unmapped line after a hold, unreadable packages, abandoned packages, SPUs with no company and stock history gaps. Other areas add their own.

_Sources: [Import design for BigSeller exports](https://github.com/byanto/niskora/issues/7), [Bounded contexts and module layout](https://github.com/byanto/niskora/issues/6), [Import rule changes from the full history](https://github.com/byanto/niskora/issues/23), [Monitoring and alerting](https://github.com/byanto/niskora/issues/27)._

## 8. The history load

- **FND-88** Once, at go-live, the [history load](../../GLOSSARY.md#imports) brings in every order and every stock movement BigSeller still holds. Orders reach back to 1 July 2024. Stock movements reach back two years, from both the History Record tab and the Record Within 90 Days tab.
- **FND-89** The history files have the same columns as the daily exports and go through the same import, with the same rules. BigSeller accepts each long range in one export.
- **FND-90** For the months that have orders and no stock movements, on hand is unknown (FND-77). Demand and location analysis use every order.

_Sources: [Import design for BigSeller exports](https://github.com/byanto/niskora/issues/7), [Export the full order and stock movement history](https://github.com/byanto/niskora/issues/19), [Import rule changes from the full history](https://github.com/byanto/niskora/issues/23)._

## 9. How figures are read

The general approach is in [engineering.md](engineering.md). These rules are the foundation's part of it.

- **FND-91** Every figure means "as known now". A later cancellation, a corrected SPU or a filled gap changes past figures. There is no as-of reporting.
- **FND-92** A query over packages, package lines or stock movements counts in SQL and returns grouped rows. Raw rows are never loaded into code to be counted.
- **FND-93** `orders` hands other modules small, aggregated results: demand per Single SKU per business day, and packages, units and goods value per destination text. No module reads another module's tables.

_Sources: [Reporting and read model approach](https://github.com/byanto/niskora/issues/15), [Bounded contexts and module layout](https://github.com/byanto/niskora/issues/6)._
