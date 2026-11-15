# niskora

The business brain that sits beside BigSeller for a multi-store Indonesian e-commerce seller. It turns BigSeller exports into a trusted catalogue, order history and stock history, and builds restock advice, purchase orders and analysis on top.

Where BigSeller has a name for a thing, niskora uses that name. niskora coins a term only for things BigSeller has no word for.

## Language

### Imports

**Export**:
A file, or a zip of files, produced by BigSeller for one data set and one filter.
_Avoid_: Dump, report, download

**Import**:
One upload of one export, accepted or rejected as a whole, with a report of what it changed.
_Avoid_: Sync, load, ingestion

**History load**:
The one-off import of all past orders and stock movements that BigSeller still holds, done at go-live.
_Avoid_: Backfill, migration

**Held package**:
A package kept out of the order history because one of its lines names a Merchant SKU the catalogue does not know yet.
_Avoid_: Failed row, quarantined order, rejected package

**Not seen**:
The state of a package or Merchant SKU that a later export should have contained but did not. It is reported and never treated as a deletion.
_Avoid_: Missing, deleted, dropped

**Stale**:
A data set whose last successful import is older than its allowed age.
_Avoid_: Outdated, expired

**Business day**:
Midnight to midnight in Jakarta time (WIB).
_Avoid_: Day, calendar day

### Catalogue

**Catalogue**:
All the Merchant SKUs niskora knows, retired ones included.
_Avoid_: Product list, supplier catalogue, inventory

**Merchant SKU**:
A sellable item identified by its Merchant SKU code, which is the key that joins every export. It is either a Single SKU or a Combination SKU.
_Avoid_: SKU, product, item, variant

**Single SKU**:
A Merchant SKU that is one physical item and holds stock.
_Avoid_: Simple SKU, base SKU

**Combination SKU**:
A Merchant SKU that is sold as one item and stands for fixed quantities of Single SKUs. It holds no stock of its own.
_Avoid_: Bundle, kit, multi-pack

**Component**:
A Single SKU in its role as part of a Combination SKU.
_Avoid_: Child SKU, bundle item

**Retired Merchant SKU**:
A Merchant SKU that is no longer in BigSeller's Merchant SKU list but is kept with all its history.
_Avoid_: Deleted SKU, inactive SKU, archived SKU

**Discontinued Merchant SKU**:
A Merchant SKU the business has decided not to buy again. It is still sold until its stock runs out and never gets restock advice. A whole SPU can be discontinued at once.
_Avoid_: Retired, inactive, dead stock, do-not-restock

**SPU**:
The Merchant SKUs that share the part of their code before the first `-`. A Merchant SKU whose code breaks that rule is an SPU of one, and a Combination SKU belongs to no SPU by its code. Every SPU belongs to exactly one company.
_Avoid_: Product, product family, parent SKU, variant group

**Company**:
One of the three businesses under the same owner (Kirana, Senja, Lumio), each owning its SPUs and its stores. Stock, demand and the warehouse are shared across all three.
_Avoid_: Brand, store, business unit, label

**Listing**:
A product page in a store that sells one or more Merchant SKUs. One Merchant SKU can have many listings, even within one store. Listings are not held in v1.
_Avoid_: Product, store SKU, marketplace product

### Orders

**Marketplace**:
The selling platform a store belongs to, as the order export names it: Shopee, Lazada, TikTok, Blibli or Manual. Every store is on exactly one.
_Avoid_: Platform, channel, Other

**Store**:
One sales channel account, identified by its BigSeller Store Nickname: a marketplace shop, or a built-in channel such as Manual Orders. Every store in an order export counts. A store belongs to at most one company and may sell any company's Merchant SKUs.
_Avoid_: Shop, channel, account, brand

**Order**:
The marketplace's own grouping of one or more packages under one Order No. It holds nothing of its own in niskora.
_Avoid_: Sale, transaction

**Package**:
One parcel of an order, identified by its Package No. It is the unit an order export is imported by and the unit demand and destinations are counted in.
_Avoid_: Shipment, parcel, order

**Package line**:
One Merchant SKU and its quantity in one package.
_Avoid_: Order line, order item, row

**Component line**:
A package line created for one component of a Combination SKU that the package contains. It sits beside the Combination SKU's own package line.
_Avoid_: Expanded line, child line

**Hub package**:
A package that moves stock to a Shopee hub for an order Shopee fulfils. It mirrors the buyer's package and is never a customer order.
_Avoid_: Matched package, FBS order, hub order

**Final status**:
A package status that will not change again: Completed, Canceled, or Return & Refund.
_Avoid_: Closed, done, terminal status

**Demand**:
The units of a Single SKU that buyers ordered, counted from the package lines and component lines of every package that is not Canceled, on the business day of its Order Time. A returned package still counts. A Combination SKU's own line, a hub package and a stock movement are never demand.
_Avoid_: Sales, units sold, consumption

**Buyer**:
The person a package was sold to, known only through the fields on that package. niskora keeps no record of a buyer across packages.
_Avoid_: Customer, client, recipient

**Destination**:
The place a package is shipped to, in the marketplace's own words and as far as it reveals them: province, city and district. A package whose destination is masked or empty has an unknown destination.
_Avoid_: Address, location, region

### Locations

**Place**:
An official province, city or district from the government's list. A city is either a kota or a kabupaten, and every district lies in one city and every city in one province.
_Avoid_: Region, area, location, regency

**Place alias**:
A spelling a marketplace uses for a place, mapped to that place automatically or by a user.
_Avoid_: Mapping, synonym, correction

**Unmatched destination**:
A destination with a spelling that maps to no place yet. Its package counts as unknown below the last level that did match.
_Avoid_: Unknown location, bad address, unmapped destination

### Stock

**Warehouse**:
The single place stock is counted for. The second building is shelves inside it, not a warehouse of its own.
_Avoid_: Location, building, gudang

**On hand**:
The units of a Single SKU physically in the warehouse. "Stock" with no other word means on hand.
_Avoid_: Physical stock, total stock, quantity

**Allocated**:
The on-hand units already promised to packages that have not left the warehouse.
_Avoid_: Reserved, locked, committed

**Available**:
On hand minus allocated.
_Avoid_: Free stock, sellable stock

**On the way**:
The units ordered from a supplier and not yet received, as BigSeller reports them.
_Avoid_: In transit, incoming, on order

**Inventory snapshot**:
BigSeller's Inventory List as exported on one business day: the stock figures and Weighted Average Cost of every Merchant SKU.
_Avoid_: Stock snapshot, stock level, balance

**Stock movement**:
One recorded change to a Single SKU's on-hand stock, with the stock before and after, its type and the document that caused it.
_Avoid_: Stock transaction, stock in/out record, inventory log

**Purchase Stock-In**:
A stock movement that records goods received from a supplier, carrying BigSeller's own purchase document number.
_Avoid_: Receipt, goods received, delivery

**Stock history gap**:
A break in a Single SKU's chain of stock movements, where one movement's stock after does not match the next one's stock before.
_Avoid_: Missing movement, hole

### Cost

**Weighted Average Cost**:
What a Single SKU's stock really cost per piece, freight included, averaged over the purchases still in stock. It is BigSeller's figure, taken as is.
_Avoid_: Actual cost, COGS, cost price

**Pricing cost**:
The deliberately padded cost the owner sets selling prices against, so prices need not follow every change in exchange rate or freight. It is set per Single SKU, and a Combination SKU's is the sum of its components'.
_Avoid_: COGS, reference cost price, standard cost

### Pricing

**Selling price**:
The normal price of one piece of a Merchant SKU on one marketplace, without any promotion. A user types it on the anchor marketplace and on independent marketplaces; on a linked marketplace it follows from the anchor's. Every store on the marketplace uses it unless the store has its own typed price. A Merchant SKU with no selling price on a marketplace is not sold there.
_Avoid_: Retail price, harga ecer, list price, channel price

**Anchor marketplace**:
The one marketplace whose selling prices the linked marketplaces follow. It is Shopee.
_Avoid_: Base marketplace, main marketplace, reference platform

**Linked marketplace**:
A marketplace whose selling prices BigSeller sets from the anchor marketplace's through a price ratio.
_Avoid_: Synced marketplace, follower

**Independent marketplace**:
A marketplace whose selling prices are typed with no tie to the anchor marketplace, such as Manual and Blibli.
_Avoid_: Unlinked marketplace, standalone

**Price ratio**:
The number the anchor marketplace's selling price is multiplied by to give the selling price on one linked marketplace. It is set per SPU, and a Merchant SKU priced differently from the rest of its SPU can have its own. niskora records the ratio a user has set in BigSeller.
_Avoid_: Multiplier, markup, linkage rule

**Marketplace category**:
The marketplace's own category that an SPU or a Combination SKU is sold under on that marketplace. It decides which fee percentages apply.
_Avoid_: Product category, kategori

**Minimum buy**:
The fewest pieces of a Merchant SKU a buyer can order at once on one marketplace. It is 1 unless set.
_Avoid_: Minimum order quantity, MOQ, minimum purchase

**Wholesale tier**:
A lower price per piece that applies from a stated quantity upward. A selling price can have several, on marketplaces that allow them.
_Avoid_: Grosir, bulk price, quantity discount

**Fee**:
An amount a marketplace charges on a sale: either a percentage of the price, which may have a cap and may vary by the marketplace's category, or a fixed amount per order.
_Avoid_: Commission, admin cost, charge

**Fee schedule**:
All the fees of one marketplace as they apply from a stated date.
_Avoid_: Fee table, rate card, marketplace fee

**Allowance**:
A percentage of the price the owner sets aside per marketplace for a cost the marketplace does not charge on every sale, such as ads or affiliates.
_Avoid_: Fee, budget, misc

**Target margin**:
The share of the price the owner wants left as profit after fees, allowances and the pricing cost.
_Avoid_: Markup, multiplier, desired profit

**Recommended price**:
The lowest price at which a Merchant SKU yields the target margin on the anchor marketplace or an independent marketplace. It is advice and never becomes the selling price by itself.
_Avoid_: Suggested price, calculated price

**Recommended ratio**:
The price ratio at which a linked marketplace yields the same planned margin as the anchor marketplace's selling price.
_Avoid_: Suggested multiplier

**Planned profit**:
What one piece earns at a price after fees and allowances, measured against the pricing cost.
_Avoid_: Net profit, margin

**Real profit**:
The same sum measured against the Weighted Average Cost.
_Avoid_: Actual profit, true margin

### Purchasing

**Supplier**:
A party the business buys Merchant SKUs from. niskora keeps the list.
_Avoid_: Vendor, seller, factory

**Supply source**:
The record that one supplier can supply one Merchant SKU. A Merchant SKU can have several, and niskora keeps them.
_Avoid_: Supplier link, supplier catalogue, supplier item

**Purchase order**:
niskora's document asking one supplier for stated quantities of Single SKUs, which may belong to different companies. It is a Draft until it is Placed with the supplier, Completed when every line is closed, or Cancelled. BigSeller's own purchase document, created when goods arrive, is not a purchase order.
_Avoid_: PO request, purchase plan, order

**Purchase order line**:
One Single SKU with its ordered quantity and goods price on a purchase order. Lines are shown grouped by SPU. A line is open from the moment the purchase order is placed until it is closed.
_Avoid_: PO item, PO row

**Placed**:
Said of a purchase order the supplier has been asked to fulfil. Only the open lines of placed purchase orders count as on order.
_Avoid_: Sent, submitted, confirmed, approved

**Goods price**:
What the supplier charges for one piece, in the supplier's own currency and without freight.
_Avoid_: Unit cost, purchase price, stock-in price

**Received quantity**:
The units of a purchase order line that were counted on arrival. niskora reads it from the Purchase Stock-In movements matched to the purchase order and it is never typed into niskora.
_Avoid_: Delivered quantity, counted quantity

**Closed line**:
A purchase order line that expects no more goods. A line closes on its first Purchase Stock-In whatever the count, unless a user marks that the rest is still coming, or when a user closes it by hand.
_Avoid_: Fulfilled line, done line

**Shortfall**:
The units by which a closed line's received quantity falls below its ordered quantity. Each shortfall ends as Refunded, as a Loss or as To be resent, and is Unresolved until then.
_Avoid_: Short delivery, missing goods, shortage

**To be resent**:
Said of a shortfall the supplier has agreed to make good with a later delivery. Its units are owed by the supplier and do not count as on order until they are put on a purchase order as a resend line.
_Avoid_: Backorder, pending resend

**Resend line**:
A purchase order line that asks the supplier for units owed from a shortfall on an earlier purchase order. It has no goods price and counts as on order like any other line.
_Avoid_: Replacement line, free line, backorder line

**Overage**:
The units by which a line's received quantity exceeds its ordered quantity.
_Avoid_: Surplus, extra goods

**Unmatched stock-in**:
A Purchase Stock-In that belongs to no niskora purchase order, either because a user has not yet confirmed its match or because the goods were bought outside niskora.
_Avoid_: Unmatched receipt, orphan receipt

**Expected arrival**:
The date a placed purchase order's goods should be in the warehouse: the day it was placed plus the lead time, unless a user sets another date. A purchase order with open lines past this date is Overdue.
_Avoid_: ETA, due date, delivery date

**Forwarder**:
The freight company that carries goods bought in China to the warehouse. The business uses several.
_Avoid_: Shipper, cargo agent, expedition

**Marking code**:
The code a forwarder gives the business to mark its goods. There is one per forwarder, the same at all of that forwarder's warehouses.
_Avoid_: Shipping mark, customer code

**Forwarder warehouse**:
A forwarder's receiving address in China, such as Yiwu or Guangzhou, where a Chinese supplier delivers the goods of a purchase order.
_Avoid_: China warehouse, drop-off point

**Forwarder invoice**:
The Chinese-language document niskora produces from a purchase order for the forwarder, headed by the marking code and listing each item with its quantity and goods price in yuan. The forwarder uses it for customs.
_Avoid_: Packing list, commercial invoice

**Daftar Pesanan**:
The Bahasa Indonesia document niskora prints from a purchase order's current lines, showing quantities and never prices. It is given to a local supplier when the purchase order is placed and printed again for the unloading team when the goods arrive.
_Avoid_: Purchase order PDF, unloading sheet, receiving sheet

**Trading name**:
The one neutral name printed as the purchasing party on the Daftar Pesanan. No company or store name ever appears on a purchase order document.
_Avoid_: Buyer name, company name

**Carton**:
The supplier's shipping box for one SPU, with a fixed number of units and fixed outer dimensions. It may mix that SPU's Merchant SKUs.
_Avoid_: Box, case, koli

**Preferred supply source**:
The one supply source of a Merchant SKU that restock advice uses by default. Another can be chosen when a purchase order is made.
_Avoid_: Main supplier, default supplier, primary source

**On order**:
The units on niskora's purchase orders that have been placed with a supplier and not yet received. It is niskora's own figure and replaces BigSeller's on the way in restock advice.
_Avoid_: On the way, incoming, in transit

**Lead time**:
The days from placing a purchase order with a supplier to the goods being in the warehouse. A supplier has one, and a supply source can have its own.
_Avoid_: Delivery time, shipping time

**Order cycle**:
The days the business normally lets pass between two purchase orders to the same supplier.
_Avoid_: Review period, order frequency

**Safety days**:
The extra days of demand kept in stock as a cushion against late deliveries and busier days.
_Avoid_: Safety stock, buffer

### Restock

**Restock advice**:
For one Single SKU, whether to reorder now and how many units, worked out from its demand, its stock and its supplier's lead time.
_Avoid_: Purchase suggestion, recommendation, alert

**Stockout day**:
A business day that a Single SKU began with no on-hand stock, so buyers could not order it.
_Avoid_: Out-of-stock day, empty day

**Countable day**:
A business day that is neither a stockout day for the Single SKU nor a campaign day. Only countable days are used to measure its sales rate.
_Avoid_: Valid day, in-stock day, selling day

**Sales rate**:
A Single SKU's demand per day over its most recent 90 countable days.
_Avoid_: Velocity, average daily outbound, run rate

**Campaign day**:
A business day a user has marked as a sales event, such as 11.11. It is left out when the normal pace of demand is measured.
_Avoid_: Promo day, spike, event

### Agent

**Agent**:
The part of niskora that answers a user's questions in plain language from niskora's own data. It reads and never changes anything.
_Avoid_: Assistant, chatbot, AI, copilot

**Conversation**:
One user's saved series of questions and the agent's answers, private to that user.
_Avoid_: Chat, thread, session

**Tool**:
One fixed, read-only query the agent can run to get figures for an answer. Every figure in an answer comes from a tool.
_Avoid_: Function, query, skill

### People

**User**:
Anyone who logs in to niskora. Every user can do everything.
_Avoid_: Account, operator, staff, admin
