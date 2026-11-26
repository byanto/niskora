# niskora v1 spec

This folder is the description of how niskora behaves. It was assembled from the closed decisions on the [decision map](https://github.com/byanto/niskora/issues/1) and is ready to be cut into build tickets. After the build it stays the living description: every pull request that changes behaviour updates the spec in the same pull request.

## Purpose

niskora is a business brain that sits beside BigSeller for one multi-store Indonesian e-commerce business. BigSeller syncs orders and stock across marketplaces. niskora takes BigSeller's own exports and answers what BigSeller does not: what to reorder, from whom and when, and where the buyers are.

niskora complements BigSeller and never replaces it. BigSeller stays the source of truth for order sync and stock quantities.

## The business it is built for

- One business, internal use: the owner, a daily operator and staff.
- Three [companies](../../GLOSSARY.md#catalogue) under the same owner, sharing one stock, one demand and one [warehouse](../../GLOSSARY.md#stock).
- Stores on Shopee, Lazada, TikTok Shop and Blibli, plus manual (mostly WhatsApp) orders.
- More than 10,000 active [Merchant SKUs](../../GLOSSARY.md#catalogue) and 300 to 500 orders a day.
- Suppliers both local and in China.

## What v1 covers

1. **Import foundation.** A person uploads BigSeller's Excel exports. niskora turns them into a clean [catalogue](../../GLOSSARY.md#catalogue), order history and stock history, and reports what each [import](../../GLOSSARY.md#imports) changed. See [foundation.md](foundation.md).
2. **Restock advice and purchase orders.** [Restock advice](../../GLOSSARY.md#restock) per [Single SKU](../../GLOSSARY.md#catalogue), and [purchase orders](../../GLOSSARY.md#purchasing) per supplier with their documents. See [restock.md](restock.md) and [purchasing.md](purchasing.md).
3. **Customer location analysis.** Where [packages](../../GLOSSARY.md#orders) go, by province, city and district, for aiming place-targeted ads. See [locations.md](locations.md).
4. **A read-only agent.** Answers questions in plain language from niskora's own data. See [agent.md](agent.md).

v1 runs on one ARM machine as one Docker Compose stack, with login and no roles. See [platform.md](platform.md).

## Settled constraints

These hold for every file in this folder.

**BigSeller**

- BigSeller has no free API. The only way data comes in is a person uploading exports through the niskora screens. No scraping and no browser automation, including BigSeller's internal JSON endpoints.
- Data up to a day old is acceptable.
- Where BigSeller has a name for a thing, niskora uses that name. [`GLOSSARY.md`](../../GLOSSARY.md) is the authority for every term, and this spec never redefines one.

**Scope of the data**

- Every [store](../../GLOSSARY.md#orders) that appears in an order export is in scope, Blibli and the built-in channels included. There is no list of allowed stores.
- There is one warehouse and one stock figure per Single SKU. The second building is shelves inside the warehouse. Stock by area or shelf is left until after v1.
- A [Combination SKU](../../GLOSSARY.md#catalogue) is virtual: a sale of one is [demand](../../GLOSSARY.md#orders) for each of its [components](../../GLOSSARY.md#catalogue).
- v1 has one cost, the [Weighted Average Cost](../../GLOSSARY.md#cost), which is BigSeller's figure with freight included. The [pricing cost](../../GLOSSARY.md#cost) is designed with pricing and built after v1.
- v1 keeps no customer record. It has [buyers](../../GLOSSARY.md#orders) and [destinations](../../GLOSSARY.md#orders) only, and a package whose destination is masked or empty has an unknown destination.
- niskora owns the [supplier](../../GLOSSARY.md#purchasing) list, the [supply sources](../../GLOSSARY.md#purchasing) and the purchase order lifecycle. Receiving stock is still entered in BigSeller ([ADR 0004](../adr/0004-received-quantities-come-from-bigseller-stock-movements.md)).
- Pricing is modelled now and built as the first feature after v1. The model is in [later/pricing.md](later/pricing.md), and v1 builds nothing for it.

**Personal data**

- All buyer data that the exports do not mask is stored. It sits apart from analysis data ([ADR 0003](../adr/0003-one-database-schema-per-module.md)) and shows only on a single package's own page.
- The agent's tools never return personal fields ([ADR 0005](../adr/0005-the-agent-answers-through-fixed-tools-and-never-writes-sql.md)).
- Real business data lives only in the gitignored `samples/` folder on the owner's machine. It is never committed and never pasted into issues, tests or documents.

**Technology**

- Java 25, a Spring Modulith modular monolith on Spring Boot 4, PostgreSQL 18, and a separate React app. Domain-driven design and test-driven development. No microservices.
- Docker Compose locally and in production.
- One repository holds the code, the spec and the map, written so it can be made public later: no secrets, no real data, documents in English.
- The UI is in English, built with translation keys so that Bahasa Indonesia can be added later.
- When a technical choice has to be made, development cost gets little weight. Quality, simplicity, robustness, scalability and long-term maintainability come first.

## Out of scope for v1

Each of these was ruled out on the map. None returns without a new decision.

**BigSeller and the marketplaces**

- Replacing BigSeller's order sync or stock locking.
- Direct integration with a marketplace Open API.
- Scraping or automating BigSeller.
- Multi-tenancy, or offering niskora to other sellers.

**Pricing and profit**

- Building pricing, profit analysis, ABC classification, dead-stock reports and campaign planning. Only the pricing model is designed.
- Promotional prices (special prices, flash sales, vouchers) and a rule for rounding recommended prices to a price ending.
- Pushing prices or price ratios to BigSeller or a marketplace.
- Loading the Google Sheets workbooks and matching their rows to Single SKUs.
- Competitor tracking.

**Stock and restock**

- Stock at area or shelf level, and storage-to-display replenishment.
- Demand forecasting (seasonality, trend, campaign uplift). The [sales rate](../../GLOSSARY.md#restock) sits behind one seam so a forecast can replace it later.
- Restock notifications outside the app, such as email or WhatsApp.

**Purchasing**

- Sending purchase orders to suppliers from niskora.
- Working out landed cost, storing exchange rates, a shipment that groups several purchase orders, a packing list, and tracking refund money.
- Forwarder freight prices by product category.
- Recording Shopee vouchers and delivery cost on a purchase, and telling which [buying channel](../../GLOSSARY.md#purchasing) was cheaper.

**Location analysis**

- Sales per head of population, trend charts per place, and city or district maps.
- Customer records and repeat-buyer analysis.

**The agent**

- Changing data, sending reminders or digests by itself, and writing its own SQL.
- Falling back to a second AI provider automatically, and paid AI usage.
- Proposing restock adjustments, and advice on items that do not sell.

**Screens**

- A Bahasa Indonesia UI.
- Tablet and phone layouts. v1 is designed for desktop and laptop.
- Roles. Every [user](../../GLOSSARY.md#people) can do everything.

## How to read this spec

| File | Covers | Rule id prefix |
| --- | --- | --- |
| `README.md` | purpose, scope, out of scope, settled constraints, reading order | none |
| [`foundation.md`](foundation.md) | imports, catalogue, orders, stock | `FND` |
| [`restock.md`](restock.md) | restock advice | `RST` |
| [`purchasing.md`](purchasing.md) | suppliers, supply sources, purchase orders, the three documents | `PUR` |
| [`locations.md`](locations.md) | customer location analysis | `LOC` |
| [`agent.md`](agent.md) | the agent | `AGT` |
| [`platform.md`](platform.md) | how niskora runs and is released, login and users, stored files, logs | `PLT` |
| [`engineering.md`](engineering.md) | module layout, testing, CI, migrations, API conventions | `ENG` |
| [`frontend.md`](frontend.md) | architecture, the screens, visual design | `FE` |
| [`build-order.md`](build-order.md) | the build steps | none |
| [`later/pricing.md`](later/pricing.md) | the pricing model, not part of v1 | `PRC` |

Suggested order: this file, then `foundation.md`, because every other area stands on it, then the area being built. `engineering.md` and `platform.md` apply to every build ticket.

**Rules of the spec itself**

- Every behaviour rule is restated here in full, with every later amendment already folded in. A builder never has to replay the ticket history.
- Every rule has a stable id made of the file prefix and a number, such as `FND-12`. An id is never reused. A removed rule keeps its id with a "removed" note.
- Terms link to [`GLOSSARY.md`](../../GLOSSARY.md). Reasons link to the decision records in [`docs/adr/`](../adr/).
- Closed tickets appear only as source notes under each section. They are history, and so are the prototype branches.
- Where two closed tickets disagreed, or left a gap, the owner settled the case in the [final review of the v1 spec](https://github.com/byanto/niskora/issues/36). The rules here already say what was settled.

_Sources: [Map: v1 spec for the business brain](https://github.com/byanto/niskora/issues/1), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17), [Assemble the v1 spec and build order](https://github.com/byanto/niskora/issues/29), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._
