# Shape of the BigSeller exports, JSON captures and owner spreadsheets

Research for issue #4. All 33 files under `samples/` were inspected on **2026-11-12** with throwaway Python scripts (openpyxl, csv, json) kept outside the repo. Every number below was measured on those files.

Ground rules for reading this document:

- **Structure only.** The samples are real business data. No customer, store, supplier, SKU, product or price values appear here. Formats are shown with synthetic placeholders (for example `ABC0123-RED-L`). The single warehouse is called "Warehouse A", the two shelf prefixes "W1" and "W2".
- **The samples are small.** Most Excel exports hold 5 to 50 rows and each JSON capture is one page of a list. A statement like "always empty" means "empty in every sample row", not "never filled".
- **Field meanings are inferred from the data**, not checked against BigSeller's help pages. Where the meaning is a guess it is marked "(inferred)".
- Sources are cited as paths relative to `samples/`. The one Google Sheets workbook has a brand name in its file name, so it is cited as `google-sheets/<workbook>.xlsx`.

## 1. Summary: answers and what they mean for the model

**Entities and links.** BigSeller exposes: store, marketplace listing (product + variation), Merchant SKU, order, package, order line, warehouse, shelf, stock per warehouse, purchase order + lines, supplier, customer (manual orders only). In the Excel exports every link is a **text key**: `BigSeller Store Nickname` for the store, the Merchant SKU code (`SKU Name` / `Merchant SKU`) for the SKU, `Package No` / `Order No` for orders, `Purchase No` for POs, the supplier name for suppliers, the warehouse name for warehouses. Numeric ids exist only in the JSON. Details in sections 3 and 4.

**Variant grouping.** BigSeller does **not** group variants. Its parent field (SPU) is empty on every Merchant SKU sampled (55 of 55). We must build the grouping. The owner's SKU code convention makes that possible: all 734 distinct Merchant SKU codes seen across all files match `FAMILY[-variant...]` where FAMILY is 2-3 letters + 4 digits, giving 162 families. See section 5.

**Customer and location.** Masking differs sharply per marketplace (section 6):

| | Name | Phone | Province / City | District | Street address | Post code |
|---|---|---|---|---|---|---|
| Shopee | masked | masked | clear | clear | masked while "New", clear once "Completed" | clear |
| TikTok Shop | masked | masked | clear | masked | masked | masked or empty |
| Lazada | masked | masked | **masked** | masked | masked | empty |
| Manual | clear | clear (customer list) | mostly empty | mostly empty | mostly empty | empty |

Customer location analysis is feasible down to district for Shopee, down to city for TikTok Shop, and **not at all for Lazada** from these exports. There is no stable buyer identifier for TikTok Shop or Lazada; Shopee has a clear buyer username.

**Natural keys.** `Package No` is the only key that was unique everywhere (21 CSV packages, 300 JSON packages) and it is what one export row or row group represents. `Order No` was also unique, but only because the samples contain no split or merged orders. **There is no order line id in the Excel exports**, and Lazada lines are exploded into identical one-unit rows, so no column combination identifies a line. The import should replace all lines of a package on re-import instead of upserting lines. See section 7.

**Same order across exports.** No sample order appears in two different states: the order CSVs form four disjoint sets of packages and none of them is in the JSON capture. How a row changes when the status changes, and what a split package looks like, could **not** be verified. This needs a second export from the owner (section 11).

**Order-level data is repeated on every line row** in the per-line order export (order total, fees, vouchers, address, times). Summing those columns over rows double counts. The per-package export has one row per package with the line values joined by line breaks inside each cell.

**Owner spreadsheets.** Purchase orders and supplier links are **not** recorded in Google Sheets or Grist at all; they live only in BigSeller. The Google Sheets workbook is a pricing calculator (COGS, retail price, one wholesale tier, per-channel price multipliers, fee rates). Grist is a competitor price tracker, and it is the only place where multi-tier wholesale prices are written down, as a text string. See section 8.

**JSON vs Excel.** JSON adds numeric ids, marketplace variation ids per order line, bundle components and unit conversions. Excel adds everything about location, fees, line cost, lifecycle times and PO lines. Neither is a superset. See section 9.

**Surprises that affect the model:**

1. There are **more stores in BigSeller than expected**: several on Shopee, Lazada and TikTok Shop, **two on Blibli**, plus three built-in pseudo-stores (manual orders, POS retail, messenger orders) (`bigseller/json/shopsAndPlatforms.json`).
2. **Only one warehouse exists in BigSeller.** Every file names the same warehouse; the JSON shows a single warehouse id. Shelf codes carry two different prefixes (W1: 43 uses, W2: 90 uses), which suggests the second physical location is modelled as shelves, not as a BigSeller warehouse.
3. The **same Merchant SKU is listed many times**: 5 SKUs map to 50 listings, 4-7 stores each, often twice in the same store (`bigseller/excel/inventory/sku_mapping-relationship.xlsx`).
4. **Marketplace fees are filled only for Shopee** in the order export. For TikTok Shop and Lazada the fee columns are empty, and the settlement columns are empty for every row.
5. **Epoch timestamps in the JSON are not real UTC instants.** They encode local wall-clock time as if it were UTC (7 hours off).
6. Some Shopee rows in the JSON are **not buyer orders** (stock sent to a Shopee fulfilment hub, 15 of 300) and carry no amount, no buyer and no payment method.
7. Cost exists in five places that do not agree (section 4.3).

## 2. Inventory of files

Row counts exclude the header row.

### BigSeller Excel / CSV exports

| File (under `bigseller/excel/`) | Format | Sheet | Rows | Cols | Grain |
|---|---|---|---|---|---|
| `inventory/merchant-sku.xlsx` | xlsx | `SKU` | 5 | 47 | one Merchant SKU |
| `inventory/inventory-list.xlsx` | xlsx | `Single SKU` | 7 | 32 | Merchant SKU x warehouse |
| `inventory/shelf-stock.xlsx` | xlsx | `sheet` | 5 | 14 | Merchant SKU x shelf |
| `inventory/export-shelf.xlsx` | xlsx | `sheet` | 224 | 6 | shelf x Merchant SKU (merged cells) |
| `inventory/sku_mapping-relationship.xlsx` | xlsx | `sheet` | 50 | 8 | listing variation -> Merchant SKU |
| `products/products_shopee.xlsx` | xlsx | `sheet1` | 34 | 21 | listing variation |
| `products/products_lazada.xlsx` | xlsx | `sheet1` | 24 | 14 | listing variation |
| `products/products_tiktok.xlsx` | xlsx | `sheet1` | 22 | 14 | listing variation |
| `purchase/purchase-order.xlsx` | xlsx | `sheet` | 123 | 38 | PO line (5 POs) |
| `purchase/purchase-suggestion.xlsx` | xlsx | `sheet` | 5 | 35 | Merchant SKU x warehouse |
| `purchase/supplier-list.xlsx` | xlsx | `sheet` | 6 | 10 | supplier |
| `orders/customer-list.xlsx` | xlsx | `sheet` | 5 | 22 (+1 empty) | customer (manual orders) |
| `orders/order-export-by-merchant-sku.csv` | csv | - | 17 | 101 | order line (7 packages, status New) |
| `orders/order-export-by-single-sku.csv` | csv | - | 17 | 101 | byte-identical to the file above |
| `orders/order-export-by-store-sku.csv` | csv | - | 17 | 101 | byte-identical to the file above |
| `orders/order-export-by-package-number.csv` | csv | - | 7 | 101 | package (same 7 packages) |
| `orders/order-export-by-merchant-sku_combination-sku.csv` | csv | - | 5 | 101 | order line with bundles (3 packages, Completed) |
| `orders/order-export-by-store-sku_combination-sku.csv` | csv | - | 5 | 101 | byte-identical to the file above |
| `orders/order-export-by-single-sku_combination-sku.csv` | csv | - | 5 | 101 | same 3 packages, bundles expanded to components |
| `orders/order-export-by-package-completed-shopee.csv` | csv | - | 5 | 101 | package (20 lines) |
| `orders/order-export-by-package-completed-tiktok.csv` | csv | - | 4 | 101 | package (15 lines) |
| `orders/order-export-by-package-completed-lazada.csv` | csv | - | 2 | 101 | package (15 unit lines) |

All ten order CSVs share the exact same 101-column header in the same order. The "orders" are CSV, not xlsx.

### BigSeller JSON captures

Each is one page of an internal list endpoint (the endpoints are listed in `bigseller/json/url.txt`). All share the envelope `{code, errorType, msg, msgObjStr, data}`.

| File (under `bigseller/json/`) | Rows in page | Total reported | Grain | Row path |
|---|---|---|---|---|
| `shopsAndPlatforms.json` | all stores | - | store | `data.allShops[]`, also `data.shops.<platform>[]` |
| `merchant-sku-pageList.json` | 50 | about 10,000 | Merchant SKU | `data.rows[]` |
| `inventory-list-pageList.json` | 50 | about 10,000 | Merchant SKU x warehouse | `data.page.rows[]` |
| `order-pageList.json` | 300 | tens of thousands | package, with nested lines (582) | `data.page.rows[]`, `orderItemList[]` |
| `purchase-order-pageList.json` | 50 | a few thousand | PO header only, no lines | `data.rows[]` |
| `active.json` | 50 | a few thousand live | Shopee listing with nested variations (157) | `data.page.rows[]`, `variations[]` |
| `url.txt` | - | - | list of the endpoint URLs | - |

### Owner spreadsheets

| File | Sheet / table | Rows | Cols | Grain |
|---|---|---|---|---|
| `google-sheets/<workbook>.xlsx` | `Daftar Barang` | 970 (732 with a SKU) | 33 | product family + variant name |
| same | `Marketplace Fee` | 3 | 6 | marketplace |
| `grist/Business Data-Stores.csv` | - | 51 | 15 | Shopee shop (3 own, 48 competitors) |
| `grist/Business Data-ProductListing.csv` | - | 281 | 16 | product title x shop |
| `grist/Business Data-SalesLog.csv` | - | 270 | 7 | listing x check date |

## 3. Per export type

Conventions used in the tables: "text" means the cell is a string even when it holds a number; "num" means a real numeric cell. `--` is BigSeller's placeholder for "no value" and is a literal two-character string.

### 3.1 Order export (CSV, 101 columns)

Source: all ten files in `bigseller/excel/orders/*.csv`. UTF-8 with BOM, CRLF line endings, comma separated, every value is text.

Two layouts share one header:

- **Per line** ("by merchant SKU", "by store SKU", "by single SKU"): one row per order line. All order-level columns are repeated on each row (verified: within a package only the line columns 26-49 vary).
- **Per package** ("by package"): one row per package. Line columns hold one value per line, joined with CRLF and with a trailing CRLF, inside one quoted cell. The value lists have equal length across line columns in every sample row.

| # | Column | Level | Format / values seen | Notes |
|---|---|---|---|---|
| 1 | Order No | order | Shopee: 14 chars, `YYMMDD` + 8 alphanumerics. TikTok: 18 digits. Lazada: 16 digits | Marketplace order number. Plain text, no scientific notation |
| 2 | Package No | package | `BS` + 4-char account code + 6 digits | BigSeller's own number. Unique in all samples |
| 3 | Order Type | order | `--` | always `--` |
| 4 | Order Mark | package | comma-separated user labels, or empty | free-text labels set by staff |
| 5 | Package Type | package | `Single Item(1 qty)`, `Single Item(multiple qty)`, `Multiple Items` | |
| 6 | Main Package No | package | `--` | presumably set on split packages; never filled |
| 7 | Invoice No. | order | empty | |
| 8 | Order Status | package | `New`, `Completed` | BigSeller status |
| 9 | Marketplace Status | order (Lazada: line) | Shopee `To Process(New)`, `Completed`; TikTok `Awaiting Shipment`, `Completed`; Lazada `Confirmed` | Lazada repeats it per line in the per-package layout |
| 10 | Marketplace | order | `Shopee`, `TikTok`, `Lazada` | capitalised here, lower case in other exports |
| 11 | Marketplace Store | order | the shop name on the marketplace | capitalisation differs between marketplaces for the same brand |
| 12 | BigSeller Store Nickname | order | `<PLATFORM CODE> - <BRAND>` | **store key**; matches the store list 100% |
| 13 | Seller Name | order | empty | |
| 14 | Username (Buyer) | order | see section 6 | |
| 15-16 | Customer Code, Customer Name | order | `--` | BigSeller CRM customer; only for manual orders (inferred), none in the CSV samples |
| 17-18 | Receiver Name, Phone Number | order | masked, see section 6 | |
| 19-24 | Post Code, Country, Province (State), City, District (Area), Town | order | see section 6 | Country is always `Indonesia` |
| 25 | Shipping Address | order | see section 6 | free text, commas inside |
| 26 | SKU | line | free text | the **store SKU** typed on the marketplace listing; not the Merchant SKU |
| 27 | Product ID | line | Shopee 10-11 digits, Lazada 10-11 digits, TikTok 19 digits | marketplace listing id. **No variation id column exists** |
| 28-29 | Product Name, Variation Name | line | text | listing title and variation label |
| 30-32 | First Level / Secondary / Third Level Category | line | text, `--` when missing | marketplace category names, language varies by marketplace |
| 33 | Quantity | line | integer as text | Lazada: always 1, one row per unit |
| 34 | Price | line | integer as text | unit price after seller discount (inferred) |
| 35 | Product Subtotal | line | integer as text | equals Price x Quantity in every row |
| 36 | Original Price | line | integer as text | list price before discount |
| 37 | Merchant SKU | line | `ABC0123-RED-L` | **SKU key**. In the "single SKU" layout a bundle is replaced by its component |
| 38 | Title | line | text | Merchant SKU title |
| 39 | Base Unit | line | empty in per-line files, `PCS` in per-package files | inconsistent between layouts |
| 40 | Serial Number | line | `--` | |
| 41 | Image Link | line | URL | |
| 42 | To Allocate/Deduct | line | integer as text | stock units to deduct. For an expanded bundle it is Quantity x pieces per bundle |
| 43 | Commodity Cost | line | 2 decimals as text | **line total cost**, not unit cost (cost / quantity is constant per SKU for all 61 SKUs seen) |
| 44 | Reference Price | line | 2 decimals as text | the Merchant SKU's reference cost price (inferred); `0.00` on about half the lines |
| 45-48 | Product Weight, Length, Width, Height | line | `0` or 2 decimals | mostly zero |
| 49 | Shelf | line | `W1-L1-R03-T2`, or `No Shelf` | bundles show `No Shelf` |
| 50 | Package Weight(g) | package | `0` | |
| 51-52 | Inspector, Salesperson | package | `--` | |
| 53 | Stock-Out | package | `Yes`, `No` | stock deducted |
| 54 | Order Fulfillment Type | package | `Seller Fulfillment` | |
| 55 | Stock-In | package | `No` | returned to stock (inferred) |
| 56 | Shipping Warehouse | package | one warehouse name | **warehouse key** |
| 57 | Buyer Designed Logistics | order | courier service name; Lazada has a trailing space | |
| 58 | Shipping Option | order | `<Marketplace>-ID-<courier service>` | |
| 59 | Cargo Carry Method | order | `Pickup`, `Dropoff`, empty | |
| 60 | Tracking Number | package | text, empty until shipped | several formats per courier |
| 61 | Shipping Fee | order (Lazada: line) | `0`, `0.0`, `9000.0`, `2000` | inconsistent decimals |
| 62 | Shipping Fee Paid By Seller | order | empty | |
| 63-64 | Seller Shipping Discount, Marketplace Shipping Discount | order (Lazada: line) | Shopee 5 decimals (`0.00000`), Lazada 1 decimal, TikTok empty | |
| 65 | Order Total | order | integer as text | does not equal the sum of line subtotals (differences of about 300 to 11,000 in the sample); formula not derived |
| 66 | Payment Method | order | `COD`, `Prepaid` | |
| 67 | Management Fee | order | integer as text | **Shopee only**; empty for TikTok and Lazada |
| 68 | Transaction Fee | order | `0` | Shopee only |
| 69-70 | Seller Discount, Marketplace Discount | order (Lazada: line) | 5 decimals; Lazada `--` for Marketplace Discount | Shopee and Lazada only |
| 71-72 | Voucher, Store Voucher | order (Lazada: line) | integer as text | |
| 73 | Currency | order | `IDR` | |
| 74-83 | Order Time, Order Paid Time, Valid Time, Expire Time, Packed Time, Printed Time, Order Shipped Time, Transit Time, Completed Time, Cancel Time | order | `DD Mon YYYY HH:mm` (English month), or empty | no seconds, no timezone |
| 84-85 | Cancellation Reason, Status Before Canceled | order | empty / `--` | no cancelled order in the samples |
| 86-89 | Buyer Message, Seller-Note, Note for CS, Note for Pick | order | empty | |
| 90 | Payment Channel | order | empty | |
| 91-92 | VAT, Service Fee | order | `--` | |
| 93 | Cancel Initiator | order | empty | |
| 94 | Platform Order No | order | empty | |
| 95-98 | Settlement Status, Settled Amount, Unsettled Amount, Credit Due Date | order | empty | never filled, even for Completed orders |
| 99-100 | Supplier Name, Supplier Code | line | `--` | |
| 101 | Is Abnormal Order | order | `No` | |

Enum values seen for couriers (`Buyer Designed Logistics`): Shopee - SPX Hemat, SPX Standard, SPX Instant, SPX Instant Prioritas, SPX Ambil di Tempat, SiCepat REG, SiCepat Ekonomis, J&T Cargo. TikTok - J&T Express, Anteraja, Grab. Lazada - `standard` with `LEX ID`. Manual orders (JSON only) use free-text couriers.

### 3.2 Merchant SKU export

Source: `bigseller/excel/inventory/merchant-sku.xlsx`, sheet `SKU`. All cells are text except `Total On Hand Stock` and `Conversion Rule1`.

| Column | Format | Notes |
|---|---|---|
| SKU Name | `ABC0123-RED-L` | **the Merchant SKU code**, natural key |
| Title | `PRODUCT NAME - RED - L` | mirrors the code: same number of ` - ` segments as the code has `-` segments (50 of 50 in the JSON) |
| Category | `No Category` | unused |
| GTIN | empty | unused |
| Cost Currency / Reference Cost Price | `IDR` / integer as text | manual reference cost; `0` on 3 of 5 rows |
| Selling Price Currency / Reference Selling Price | `IDR` / integer as text | `0` on 3 of 5 |
| Promo Price / Promo Price Currency | `0` / `IDR` | unused |
| SKU Alias, Brand, Material, Usage, Tag | empty | unused |
| Total On Hand Stock | num | across warehouses |
| Date of Sale | `DD Mon YYYY` or empty | |
| Net Weight(g), Gross Weight(g), Length/Width/Height(cm), Volume(cm3) | text numbers, mostly `0` | |
| Image URL | URL | |
| Merchant SKU Note | empty | |
| SKU Type | `Single SKU` | no bundle in this sample |
| Merchant Code | 8 digits | BigSeller-generated code; equals `merchantSkuId` in the JSON (5 of 5) |
| Creation time | `YYYY-MM-DD HH:mm` | a different date format from the other exports |
| SPU Number | `--` | **the parent grouping field, unused** |
| Multi-Unit Management | `Enable` | |
| Base Unit, Purchase Unit | `PCS` | |
| Conversion Unit1..5, Conversion Rule1..5, Barcode1..5 | unit name, number, empty | up to 5 alternative units, for example a dozen = 12 base units |

JSON counterpart (`bigseller/json/merchant-sku-pageList.json`, `data.rows[]`): `id` (numeric SKU id), `sku`, `title`, `merchantSkuId`, `spu` (null x50), `spuId` (0 x50), `isGroup` (0 x50), `skuType` (0 x50), `productCount` (number of mapped listings, 1 to 10+), `mappingStatus`, `referencePrice`, `referenceSalePrice`, `totalHand`, `createTimeStr`, `updateTimeStr`, `basicUnitName`, `createSkuRelationExportDTO.skuUnitRelation[]` (`unitName`, `unitNumber`).

### 3.3 Inventory list

Source: `bigseller/excel/inventory/inventory-list.xlsx`, sheet `Single SKU` (the sheet name suggests bundles would come on another sheet; not observed).

| Column | Format | Notes |
|---|---|---|
| Image URL | URL | first column, before the key |
| SKU Name, Title | text | Merchant SKU key |
| GTIN, Unit | empty, `PCS` | |
| Weight(g), Length/Width/Height(cm) | text, 2 decimals | |
| Warehouse | name | **warehouse key**; one value |
| Area, Shelf | `--` | not filled at this grain |
| On Hand, Order Allocated, Available for whole warehouse, Promo Reserved, On The Way, Stock Alert | num (float) | Available = On Hand - Allocated in every row |
| Whole Warehouse Unstored, Defective Stock | integer as text | mixed typing within one sheet |
| On The Way Total Cost, Total Cost of Defective Products | text, 2 decimals | |
| Forecasted Daily Sales | text, 2 decimals | BigSeller's forecast |
| Estimated Available-for-Sale Days | integer as text or `--` | |
| Weighted Average Cost | text, 2 decimals | moving average cost per unit |
| Total Cost | text, 2 decimals | = cost x on hand (50 of 50 in the JSON) |
| Sales Status | `Active` | |
| Primary / Secondary / Tertiary Category | `No Category`, empty | unused |
| SKU Type | `Single SKU` | |
| Note | empty | |

JSON counterpart (`bigseller/json/inventory-list-pageList.json`, `data.page.rows[]`): `warehouseSkuId` (id of the SKU x warehouse row), `skuId`, `sku`, `merchantSkuId`, `warehouseId`, `warehouseName`, `onhand`, `allocated`, `available`, `promoReservedStock`, `onTheWay`, `cost` (weighted average, text), `totalCost`, `avgDailySales`, `unshelved`, `defective`, `saleState`, `isGroup`, `unitCommonVOList[]` (`unitName`, `conversionQuantity`, `basicUnitFlag`, `purchaseUnitFlag`). `purchaseDays` and `safetyDays` are null here.

### 3.4 Shelf stock and shelf list

`bigseller/excel/inventory/shelf-stock.xlsx` (all text): SKU Name, GTIN (`--`), Unit, SKU Title, Warehouse, Area (`Default Storage Area`), Shelf (`W2-L2-K01-T1` style), Shelf Type (`Storage Shelf`), Shelf On Hand Stock, Shelf Allocated (`--`), Shelf Available Stock, No stock added on shelf, Upper Restock Limit (`--`), Lower Restock Limit (`--`). Grain: SKU x shelf.

`bigseller/excel/inventory/export-shelf.xlsx`: Warehouse, Shelf Name, Area (`Default Storage Area`, `Default Picking Area`), Shelf Type (`Storage Shelf`, `Picking Shelf`), Merchant SKU, Note. 5 shelves, 223 SKU rows plus 1 shelf with no SKU. **Shelf Name, Area and Shelf Type are vertically merged cells**: the value is present only on the first row of each shelf block (219 of 224 rows are blank) and must be filled down. Warehouse is repeated on every row.

Shelf code format: `<prefix>-L<n>-<R|K..><nn>-T<n>`, four dash-separated segments, where the first segment takes two values (W1, W2).

### 3.5 SKU mapping relationship

Source: `bigseller/excel/inventory/sku_mapping-relationship.xlsx`. All text. This is the bridge between marketplace listings and Merchant SKUs.

| Column | Format | Notes |
|---|---|---|
| Merchant SKU Name, Merchant SKU Title | text | Merchant SKU key |
| Platform | `shopee`, `lazada`, `tiktok`, `blibli` | lower case |
| Store Nickname | text | store key, matches the store list 50 of 50 |
| Product Name | text | listing title |
| Product ID | Shopee/Lazada 10-11 digits, TikTok 19 digits, Blibli `AAA-00000-00000` | |
| Store SKU | free text | never equal to the Merchant SKU (0 of 50); starts with the family code in 21 of 50 |
| Variation ID | Shopee/Lazada 12 digits, TikTok 19 digits, Blibli product id + `-00001` | unique (50 of 50) |

Grain and key: (Platform, Store Nickname, Product ID, Variation ID), unique in the sample. One Merchant SKU has 6 to 14 listings over 4 to 7 stores; 21 of 27 (SKU, store) pairs have 2 or 3 listings.

### 3.6 Listing exports per marketplace

Sources: `bigseller/excel/products/products_shopee.xlsx`, `products_lazada.xlsx`, `products_tiktok.xlsx`. Grain: one row per variation; product-level columns repeat.

| Column | Shopee | Lazada | TikTok | Notes |
|---|---|---|---|---|
| Store Nickname | yes | yes | yes | store key |
| Category_L1..L3 | yes | yes (L3 often empty) | yes | English on Shopee and Lazada, Indonesian on TikTok |
| Product Name | yes | yes | yes | |
| Product id | `Item ID` | `Product ID` | `Product ID` | **column name differs** |
| Sales, Likes, Views | yes | - | - | num |
| Parent SKU | yes | yes | **absent** | family code, or several joined by `/` |
| Variation, Variation ID | yes | yes | yes | Variation ID unique (80 of 80) |
| SKU | yes | yes | yes | store SKU |
| Stock, Price | num | num | num | listing stock and list price |
| Special Price | empty | empty | empty | |
| Purchase Limit, Pre-Order, Shipping Time | yes | - | - | |
| Create Time of Marketplace | `DD Mon YYYY HH:mm` | same | same | |
| activity information | yes | - | - | multi-line text, see below |
| Promotion Name | - | - | empty | |

`activity information` (Shopee) is a multi-line cell made of blocks separated by blank lines, each block with the lines `Campaign Type:`, `Campaign Name:`, `Promo Price:IDR <n.n>`, `Campaign Time:<start> ~ <end>`, `Time Zone:`, `Campaign Status:`. Campaign types seen: `Wholesale` (3 blocks per cell, one per tier) and `Other` (1 block). The wholesale blocks give the tier prices but **not the tier quantity thresholds**.

JSON counterpart (`bigseller/json/active.json`, Shopee only): per listing `id`, `itemId`, `shopId`, `shopName`, `name`, `itemSku` (parent SKU), `status`, `shopeeStatus`, `originalPrice`, `stock`, `sales`, `views`, `likes`, `categoryId`, `link`, `createTime`, `updateTime`, `platformCreateTime`; per variation `id`, `variationId`, `variationSku`, `originalPrice`, `price` / `promotionPrice` (filled on 8 of 157), `stock`. The variation name is **not** in the JSON. `data.counter` gives listing counts per status (live, sold out, unlisted, deleted).

### 3.7 Purchase order export

Source: `bigseller/excel/purchase/purchase-order.xlsx`. Grain: one row per PO line; header columns repeat on every line (verified for supplier, tracking number, status, create time, note, total amount).

| Column | Level | Format | Notes |
|---|---|---|---|
| Purchase No | header | `PO` + 4-char account code + 6 digits | **PO key**; equals `commonNo` in the JSON (5 of 5) |
| 1688 Order No | header | empty | |
| Receiving Warehouse | header | name | |
| SKU Name, Title, Image URL | line | text | Merchant SKU key; a SKU appears once per PO |
| Weight(g), Size (cm) | line | text; size as `L*W*H` | |
| Unit Price | line | text, 2 decimals | |
| Purchase Qty, Received Qty | line | num | |
| Value, Purchase Receipt Value | line | text, 2 decimals | qty x unit price |
| Single Allocated Expense | line | `0.00` | share of fees per unit |
| Stock-in Cost Price | line | text, 2 decimals | equals Unit Price on 123 of 123 rows (no fees recorded) |
| Currency | header | `IDR` | also for purchases from Chinese suppliers |
| Total | header | num | total quantity; equals the sum of line quantities |
| Ship Fee, Other Fee, Tax Fee, First-mile Ship Fee | header | `0.00` | never used |
| Settlement Method | header | `Instant Pay` | |
| Total Amount | header | text, 2 decimals | equals the sum of line values |
| Supplier | header | text | supplier name; often a generic placeholder |
| Contacts, Phone | header | text | copied from the supplier record; **repurposed** to hold a product category and a city |
| Creator, Purchaser | header | account email | personal data, do not import |
| Shipping Method | header | `Sea Freight`, `Land Freight` | |
| Tracking No | header | free text | courier tracking or a freight-forwarder invoice reference |
| Estimated Arrival Date | header | `DD Mon YYYY` or empty | empty on 100 of 123 |
| Submit Time, Ship Time, Receiving Time, Create Time | header | `DD Mon YYYY HH:mm` | Submit, Ship and Receiving fall within minutes of each other: POs are entered at receiving time |
| Platform Order Time | header | empty | |
| Note | header | free text | **repurposed**: `Order No: <marketplace order number>` and/or `By. <staff names>` |
| Status | header | `Completed` | only completed POs sampled |

JSON counterpart (`bigseller/json/purchase-order-pageList.json`): header only - `id`, `commonNo`, `state` (3 on all rows; the capture was filtered to that state), `skuQty` (number of lines), `purchaseQty`, `receivedQty`, `paymentAmount`, `subTotal`, `arrivalAmount`, `currency`, `supplier`, `warehouse`, `warehouseId`, `trackingNo`, `trackingVos[]`, `note`, `estimatedArrivalTime` (`YYYY/MM/DD` or empty), create/submit/shipped/received/update times. No lines, no supplier id.

### 3.8 Purchase suggestion

Source: `bigseller/excel/purchase/purchase-suggestion.xlsx`. Grain: Merchant SKU x warehouse.

SKU Name, Title, Supplier (`--` on 5 of 5), Warehouse, On Hand, Available, Allocated, On The Way (num), Purchase Days and Safety Days (num; lead time and safety stock days, values 7/60 and 3/7), 3/7/15/30/60/90-Day Total Sales (num), Average daily sales for the last 3/7/15/30/60/90 days (text, 2 decimals), Forecasted Daily Sales (num), Stock Estimated Available-for-Sale Days, Estimated Available-for-Sale Days of Stock On the Way, Out of Stock Date (`DD Mon YYYY` or `--`), Out of Stock Qty, Proposed Qty (text), Planned Qty (num), Latest Stock In Date, Note, Reference Cost Price, Sales Status, Restock Strategy (`Default Replenishment Strategy`), Image Link.

This is the only export with **sales velocity windows and lead-time parameters**, which is exactly the input a restock rule needs.

### 3.9 Supplier list

Source: `bigseller/excel/purchase/supplier-list.xlsx`. 6 rows.

Supplier Name (convention `<source> - <shop handle>`, where source is `1688` or `Online`), Supplier Type (`1688 Supplier`, `Ordinary Supplier`), Owner (account), Supplier Website (a 1688 or Shopee shop URL), Company Address, Contact Information (**repurposed** as product category), Settlement Method, Phone Number (**repurposed** as city), Email Address (empty), Note (Chinese company name, non-Latin characters). There is no supplier code column in this export.

### 3.10 Customer list

Source: `bigseller/excel/orders/customer-list.xlsx`. 5 rows; BigSeller CRM customers used for manual orders.

Name, Customer Code (`CN` + 8 digits), Customer Level (`No Level`), Gender (empty), Phone (`+62...`, clear), Email (empty), Total Orders (num), Total Spend (num), First Order Time and Last Order Time (`YYYY-MM-DD HH:mm:ss`), Note, Detailed address (multi-line free text), Town, District, City, State/Province, Country/Region, Post Code, Facebook, WhatsApp, Telegram, Salesperson. Address fields are filled on 1 of 5 rows. The header row has a 23rd, unnamed empty column.

### 3.11 Store list (JSON only)

Source: `bigseller/json/shopsAndPlatforms.json`. `data.allShops[]`: `id` (numeric, 6-7 digits; the three pseudo-stores use 0, 1, 2), `name` (the BigSeller Store Nickname, unique on every row), `platform` (`shopee`, `lazada`, `tiktok`, `blibli`, `manual`, `offline`, `chat`), `site` (`ID` or null). There is no Excel export of stores.

### 3.12 Order list (JSON)

Source: `bigseller/json/order-pageList.json`. 300 packages: 268 Shopee, 18 manual, 12 TikTok, 2 Lazada. Key fields:

- Package: `id` (BigSeller numeric id), `packageNo`, `platformOrderId`, `platformOrderType` (0, 3, 4), `shopId`, `shopName`, `platform`, `state` (`shipped`, `platformProcessing`, `completed`, `pickup`), `marketPlaceState` (`Shipping`, `In transit`, `Processed`, `Delivered`, null), `platformState` (`SHIPPED`, `TO_CONFIRM_RECEIVE`, `PROCESSED`, `IN_TRANSIT`, `DELIVERED`; for Lazada a JSON document inside a string), `packState` (numeric), `amount` (text, or `--`), `paymentMethod`, `buyerUsername`, `contactPerson`, `recipient`, `trackingNo`, `shippingCarrierName`, `buyerShippingCarrier`, `manualLogistics`, `warehouseId`, `shipmentWarehouse`, `labelList[]` (user marks), `splitOrder`, `multipleOrder`, `hasDeducted`, plus about ten timestamp pairs (`xTime` epoch ms + `xTimeStr`).
- Line (`orderItemList[]`): `id` (unique, 582 of 582), `skuId` and `inventorySku` / `inventorySkuTitle` (Merchant SKU), `varSku` (store SKU), `varAttr` (variation name), `itemSku` (parent SKU of the listing), `itemName`, `platformItemId`, `platformVariationId`, `quantity`, `varDiscountedPrice`, `varOriginalPrice`, `amount`, `allocated`, `available`, `group` (bundle flag), `varSkuGroupVoList[]` (bundle components: `varSku`, `num`, `costAllocationRatio`).

Special rows:

- `platformOrderType` 3 (15 Shopee rows): the "receiver" is a Shopee distribution hub, `amount` and `paymentMethod` are `--`, there is no buyer, the order number has 19 characters instead of 14. These look like stock sent ahead to Shopee's hub (inferred).
- `platformOrderType` 4 (9 Shopee rows): a normal buyer and amount, but stock is not deducted (`hasDeducted` 0), no label is printed, and `packageIndex` holds a marketplace package number. These look like buyer orders fulfilled from that hub stock (inferred).
- Manual orders: `platformOrderId` is `YYMMDD` + 2 letters + 4 digits (BigSeller-generated), store SKU equals Merchant SKU (28 of 28 lines), 17 of 18 carry an image attachment, `amount` equals the sum of line amounts.

## 4. How the entities connect

### 4.1 Relationship picture

```
Store (nickname, platform)                          Supplier (name only)
  |                                                    |  (free-text name, weak)
  | 1..n                                               |
Listing (Product ID)  --parent SKU (text hint)-->  [Family, derived]
  | 1..n                                               ^
Variation (Variation ID, store SKU)                    | code prefix
  | n..1  (sku mapping export)                         |
Merchant SKU (code) ---------------------------------- +
  |  \__ bundle component (n..1, qty per bundle; order JSON only)
  |  \__ unit conversion (dozen, pack)
  |
  +-- Stock (SKU x warehouse): on hand, allocated, weighted avg cost
  +-- Shelf stock (SKU x shelf)        Warehouse (name) -- Shelf (code)
  +-- PO line (PO x SKU): qty, unit price    -- PO (Purchase No) -- Supplier
  +-- Order line (package x listing variation x SKU): qty, price, line cost
                                               |
                       Package (Package No) -- Order (Order No) -- Store
                                               |
                                    Receiver / location (masked per marketplace)
```

### 4.2 Join keys and measured match rates

The Excel samples were filtered differently, so a low rate between two samples mostly reflects sampling, not a broken key. Rates that test the key itself are in bold.

| From | To | Key | Match |
|---|---|---|---|
| order JSON `shopId` | store list `id` | numeric id | **300 of 300** |
| order JSON `shopName` | store list `name` | nickname | **300 of 300** |
| listing JSON `shopId` / `shopName` | store list | id / nickname | **50 of 50** |
| SKU mapping `Store Nickname` | store list `name` | nickname | **50 of 50** |
| Merchant SKU JSON `id` | inventory JSON `skuId` | numeric id | **50 of 50** (also by `sku` and by `merchantSkuId`) |
| Merchant SKU xlsx `SKU Name` | Merchant SKU JSON `sku` | code | **5 of 5** (and `Merchant Code` = `merchantSkuId` 5 of 5) |
| Merchant SKU JSON `totalHand` | inventory JSON `onhand` | - | **50 of 50** equal |
| order line `inventorySku` | order line `skuId` | - | **1:1** (350 codes, 350 ids) |
| order line store SKU (`varSku`) | Merchant SKU | - | **functional**: 359 store SKUs each map to exactly one Merchant SKU |
| PO xlsx `Purchase No` | PO JSON `commonNo` | PO number | **5 of 5**; line count, total qty and total amount all agree |
| order line `skuId` | Merchant SKU JSON page | id | 9 of 582 (the page holds 50 of about 10,000 SKUs) |
| listing xlsx `Variation ID` | SKU mapping `Variation ID` | variation id | 3 of 80 (different samples) |
| listing JSON `variationId` | listing xlsx `Variation ID` | variation id | 20 of 157 |
| order line `platformVariationId` | SKU mapping `Variation ID` | variation id | 0 of 554 (different samples) |
| order CSV packages | order JSON packages | Package No | 0 of 21 (different date ranges) |
| PO `supplier` | supplier list `Supplier Name` | name | 1 of 50 JSON POs, 0 of 5 xlsx POs (the supplier list is a 6-row sample) |
| manual order buyer name | customer list `Name` | name | 2 of 18 (customer list is a 5-row sample) |
| listing store SKU | any Merchant SKU code | text | **0 of 80** - store SKU is never the Merchant SKU |

Consequences:

- The **Merchant SKU code is the universal join key** in Excel. It behaved well: no leading or trailing spaces, no case-insensitive duplicates among 734 codes. It does contain spaces (142 codes), `#` (216), `/` (53) and lower-case letters (52), and is up to 30 characters long, so it must be stored and compared verbatim.
- The **store nickname is the only store key in Excel**. If the owner renames a store in BigSeller the key changes. The nickname embeds the platform as a prefix, but the `Marketplace` / `Platform` column should be used instead of parsing it.
- The listing-to-SKU link is only available in the SKU mapping export (or per order line in the JSON, which carries both `platformVariationId` and `inventorySku`). The order CSV carries Product ID and store SKU but **no Variation ID**.

### 4.3 Cost: five sources

| Source | Field | Grain | Observed |
|---|---|---|---|
| Merchant SKU | Reference Cost Price | SKU | manual; zero on 21 of 50 JSON rows |
| Inventory list | Weighted Average Cost | SKU x warehouse | system-maintained; zero on 3 of 50; equals the reference cost on only 6 of 50 |
| PO line | Unit Price / Stock-in Cost Price | PO x SKU | identical to each other on 123 of 123 because no freight or fees are entered |
| Order line | Commodity Cost | line total | cost at the time of the order (inferred); unit value constant per SKU within the sample |
| Google Sheets | COGS | family + variant name | manual; 15 cells are formulas such as `=<qty>*<price>` |

Freight, tax and other landed-cost fields exist in the PO export but are always `0.00`, so no source contains landed cost.

### 4.4 Supplier links

There is **no SKU-to-supplier link anywhere**. `Supplier` is `--` on every purchase-suggestion row, `Supplier Name` is `--` on every order line, and the Merchant SKU export has no supplier column. The only evidence is historical: which supplier name appeared on the POs that contained a SKU. Even that is weak, because 36 of 50 POs in the JSON use a single generic supplier name, with the real source (a marketplace order number) typed into the note. 14 distinct supplier names appear across 50 POs.

## 5. Variant grouping

BigSeller's own grouping is not in use:

- `SPU Number` is `--` on 5 of 5 rows (`bigseller/excel/inventory/merchant-sku.xlsx`); `spu` is null and `spuId` is 0 on 50 of 50 rows (`bigseller/json/merchant-sku-pageList.json`).
- Categories are `No Category` on every row of every inventory export. Brand and tag are empty.

Grouping can be derived from three signals:

1. **Merchant SKU code convention** (strongest). All 734 distinct codes match `^[A-Z]{2,3}\d{4}(-.+)?$`. The part before the first `-` is a family code: 7 distinct letter prefixes, 162 families, 1 to 10+ variants each (57 families were seen with a single variant, which is partly a sampling effect). 14 codes are a bare family code with no variant. Variant depth: 469 codes have one segment after the family, 244 have two, 7 have three. Segments are not typed: the same position holds a colour in one family and a size or a design number in another, and segments themselves contain spaces, `#`, `/` and `x` (as in `10x15`). **Split on the first `-` only**; anything finer needs per-family rules or owner input.
2. **Title convention**. The Merchant SKU title is `NAME - variant - variant`, with the same number of ` - ` separated parts as the code (50 of 50). The first part is a usable family name.
3. **Marketplace parent SKU**. Shopee and Lazada listings carry `Parent SKU` (`itemSku` in JSON), which is the family code. On order lines it is a prefix of the Merchant SKU in 551 of 562 cases. But one listing can combine several families (`AB0001/AB0002/AB0003/AB0004` on 12 of 34 Shopee rows and 4 of 50 JSON listings), 22 of 80 xlsx rows have none (TikTok has no such column), and the same family is listed in up to 4 stores. Treat it as a cross-check, not as the source.

Bundles interact with families: a multi-pack is its own Merchant SKU whose code is the component's code plus a pack suffix (for example `ABC0123-RED-PACK 20`), so it falls in the same family as its component. The family model must distinguish "variant" from "pack of a variant".

The Google Sheets pricing workbook confirms the family as the owner's unit of thought: its `SKU` column holds bare family codes (368 distinct over 732 rows) and the variant lives only in the product name.

## 6. Customer and location fields per marketplace

Sources: all `bigseller/excel/orders/*.csv` (columns 14-25) and `bigseller/json/order-pageList.json`. Placeholders are synthetic.

| Field | Shopee | TikTok Shop | Lazada | Manual |
|---|---|---|---|---|
| Username (Buyer) | **clear** login name | masked receiver name (same as Receiver Name) | masked `a*b` | n/a in CSV; JSON has the clear customer name |
| Receiver Name | first and last letter: `A******i` | per word: `a***bc d***e` | `a*b` | clear (JSON `contactPerson`) |
| Phone Number | last two digits: `******12` | `(+62)812******12` | `8*2` | clear in the customer list only |
| Province (State) | clear, UPPER CASE | clear, Title Case | **masked** `X*y` | filled on 1 of 5 customers |
| City | clear, `KAB. X` / `KOTA X` | clear, `Kab. X` / `Kota X` | **masked** | same |
| District (Area) | clear, UPPER CASE | masked `Ab*********` | masked | same |
| Town | empty | masked `Ab****` | empty | empty |
| Post Code | clear, 5 digits | `12***` or empty | empty | empty |
| Shipping Address | status New: first characters replaced by `******`, rest clear. Status Completed: **fully clear** | all parts masked except city and province | all parts masked, only the country is clear | free text, mostly empty |
| Country | `Indonesia` | `Indonesia` | `Indonesia` | `Indonesia` or empty |

Notes:

- The masking is whatever the marketplace returns to BigSeller at that moment. Shopee addresses become **more** visible after completion in this sample (5 of 5 completed packages clear, 7 of 7 new packages masked). The direction over longer periods is unknown; marketplaces usually re-mask old orders.
- The JSON list is poorer than the CSV for location: it has only `recipient`, a single string `PROVINCE, Indonesia` (Shopee, TikTok), masked for Lazada, null for manual orders. No city, district, post code, address or phone.
- Province and city spellings differ per marketplace (case, `KAB.` vs `Kab.`, and Shopee's long-form province names with an abbreviation in parentheses). A reference table of Indonesian regions is needed to normalise them.
- Buyer identity for repeat-customer analysis: only Shopee's username is stable and clear. TikTok and Lazada give nothing usable. Manual customers have a `Customer Code` in the customer list, but the order CSV samples contain no manual order, so the link from order to customer code is unverified.
- No sample contains a Blibli, POS or messenger order.

## 7. Natural keys and idempotent re-import

### 7.1 Keys per entity

| Entity | Candidate key in Excel | Uniqueness measured | Verdict |
|---|---|---|---|
| Store | BigSeller Store Nickname | distinct on every row | usable; mutable by the owner |
| Merchant SKU | SKU Name (code) | unique in every file; 1:1 with numeric id in JSON | usable; also keep `Merchant Code` (8 digits, system-generated) as a rename-proof secondary key where the export has it |
| Listing variation | (Platform, Store Nickname, Product ID, Variation ID) | 50 of 50 and 80 of 80 | usable. Variation ID alone was also unique, but id spaces differ per marketplace |
| Stock | (SKU Name, Warehouse) | unique | a snapshot, not an event: key it by export time as well |
| Shelf stock | (SKU Name, Warehouse, Shelf) | unique | snapshot |
| PO | Purchase No | 5 of 5, 50 of 50 | usable |
| PO line | (Purchase No, SKU Name) | 123 of 123 | usable in this sample |
| Supplier | Supplier Name | 6 of 6 | weak: free text, generic placeholders |
| Customer | Customer Code | 5 of 5 | usable |
| Package | Package No | 21 of 21 (CSV), 300 of 300 (JSON) | **the primary order key** |
| Order | (Store, Order No) | 21 of 21, 300 of 300 | usable, see caveats |
| Order line | none | see below | **no key** |

### 7.2 Order lines have no key

- The CSV has no line id. (Package No, Product ID, store SKU, Variation Name) and (Package No, Merchant SKU) are unique on every Shopee and TikTok line in the samples.
- **Lazada breaks every combination**: each unit is its own row with Quantity 1, so a line of 5 units is 5 identical rows (9 duplicate rows among 15 in `order-export-by-package-completed-lazada.csv`). The JSON aggregates the same kind of line into one item with a quantity (2 Lazada packages, 6 items, quantities 1 to 4).
- In the "single SKU" layout a bundle line is rewritten to its component, so the same order line has a different Merchant SKU depending on the export layout.

Recommended approach: treat a package as an aggregate. On import, compare the incoming package with the stored one and replace its lines as a set. Aggregate identical rows (sum the quantity) before storing so that Lazada behaves like the others.

### 7.3 Order number vs package number

- Package No has a constant prefix (`BS` + account code) and a number that increases with BigSeller's internal id.
- Order No formats: Shopee 14 alphanumerics (19 for the hub-stock rows), TikTok 18 digits, Lazada 16 digits, manual 12 characters. All are stored as text in the CSV. The numeric ones exceed 15 digits, so any tool that converts them to numbers will corrupt them.
- Every package maps to exactly one order and every order to one package in the samples: `Main Package No` is `--` on all rows, and `splitOrder` and `multipleOrder` are false on 300 of 300. **Split and merged packages were not observed**, so the true order-to-package cardinality is unverified. The model should assume one order can have several packages and one package can (after a merge) hold several orders, until the owner confirms otherwise.

### 7.4 Overlap between exports and status changes

- The ten order CSVs hold four disjoint sets of packages: 7 "New" packages (exported in four layouts), 3 "Completed" packages with bundles (three layouts), and 5 + 4 + 2 "Completed" packages per marketplace. No package appears in two sets, and none of the 21 is in the JSON capture.
- The three "by merchant SKU", "by store SKU" and "by single SKU" files of the first set are byte-identical, and so are two of the three in the bundle set. The layout option changes the output only when a bundle is present.
- Therefore the question "how does the same order look after a status change" cannot be answered from the samples. From the structure we can only say which columns will change: Order Status, Marketplace Status, Tracking Number, the lifecycle times, Stock-Out, the address masking (section 6) and probably the fee columns.
- An order row carries no "last updated" value. To detect change the import has to compare content (a hash of the package's normalised rows) or always overwrite.

### 7.5 Snapshots vs events

Stock, shelf stock, purchase suggestion and listing exports are point-in-time snapshots with no export timestamp inside the file. The import must record the upload time (or ask the person for the export date) and must not treat a missing SKU as zero stock, because exports can be filtered.

## 8. Google Sheets and Grist

### 8.1 Google Sheets workbook: pricing calculator

Source: `google-sheets/<workbook>.xlsx`, 2 sheets.

**Sheet `Daftar Barang`** ("item list"). Two header rows: row 1 holds six merged group titles (a channel name plus a date in parentheses), row 2 the column names, data starts at row 3. 970 formatted rows, 732 with a SKU, the rest empty formula rows. 9,551 formula cells. Column names repeat across groups (`Harga Ecer` three times, `Multiplier` twice, `Minimum Buy Ecer` three times), so columns must be addressed by position or by (group, name).

| Cols | Group | Columns | Kind |
|---|---|---|---|
| A-E | TikTok | Category_L1..L3; `Komisi Dinamis`, `Komisi Platform` (commission rates) | input, percentages |
| F-K | Shopee | Kategori_L1..L3; `Biaya Admin` (admin fee rate); two `Biaya Ongkir Xtra` rates (free-shipping programme, normal and special size, with a rupiah cap named in the header text) | input, percentages |
| L-N | - | `SKU` (family code), `Nama Produk` (name incl. variant), `COGS` | input |
| O-R | Shopee retail | `Harga Ecer` (retail price), `Net Profit Ecer`, `Profit Margin Ecer`, `Minimum Buy Ecer` | price and minimum quantity are inputs, profit and margin are formulas |
| S-W | Shopee wholesale | `Harga Grosir`, `Net Profit Grosir`, `Profit Margin Grosir`, `Grosir Quantity`, `Total Net Profit Per Order` | price and quantity are inputs |
| X-AB | TikTok retail | `Multiplier`, `Harga Ecer`, `Net Profit Ecer`, `Profit Margin Ecer`, `Minimum Buy Ecer` | all formulas except minimum buy |
| AC-AG | Lazada retail | same five columns | same |

**Sheet `Marketplace Fee`**: one row per marketplace (Shopee, Tiktok, Lazada) with `Biaya Proses Pesanan` (order processing fee, rupiah per order), `Biaya Logistik` (logistics fee, rupiah per order), `Afiliasi` (affiliate, percent), `Iklan` (ads, percent), `Misc` (percent).

How the pricing model works (read from the formulas in row 7):

- Net profit per unit = `(price x min_qty x (1 - sum of percentage fees) - fixed fees per order) / min_qty - COGS`. Fixed per-order fees are spread over the minimum purchase quantity. Percentage fees are the per-row category rates plus the per-marketplace affiliate, ads and misc rates.
- Margin = net profit / price.
- **Shopee is the base channel.** The TikTok and Lazada prices are derived: `Multiplier = ROUNDUP((COGS + fixed fees / min_qty) / (1 - (that channel's percentage fees + the Shopee retail margin)) / Shopee price, 2)` and the channel price is `Shopee price x Multiplier`. In words: price the other channels so they earn the same margin as Shopee retail.
- **Wholesale is a single tier** per row: one price and one quantity threshold (224 rows have it; 11 distinct thresholds).
- Only the "normal size" shipping-programme rate (column J) is used by the formulas; the "special size" rate (column K) is not referenced.
- The Lazada columns reuse the **Shopee** category rates (columns I and J); there are no Lazada-specific category rates in the sheet.
- Fee rates are entered **per product row** by category, not looked up from a category table. Many rows share a rate but floating-point noise is present (`0.08` and `0.0800000000000001` are different cells).

Coverage and keys:

- `SKU` holds family codes only (shape: 3 letters + 4 digits, two letter prefixes). 368 distinct codes over 732 rows; 80 codes appear on more than one row, once per priced variant. The variant is only in `Nama Produk` (444 of 708 names contain ` - `). There is **no column holding the full Merchant SKU**, so a sheet row cannot be joined to a BigSeller Merchant SKU by key, only by family code plus fuzzy name.
- Of 123 family codes seen in BigSeller orders and SKU pages, 68 are in the sheet. All 55 missing ones belong to the six 2-letter prefixes: the workbook covers one product line (the 3-letter prefixes) and not the others.
- 482 rows have a retail price; 250 have a SKU but no price.
- Fill-down gaps: category cells are empty on about a quarter of the rows while the rates are filled.

Not in the workbook: purchase orders, suppliers, stock, per-store prices (prices are per marketplace, not per store), Blibli, more than one wholesale tier, any history (the date in the group titles is the only version marker).

### 8.2 Grist: competitor and listing tracker

Sources: `grist/Business Data-Stores.csv`, `-ProductListing.csv`, `-SalesLog.csv`. UTF-8 without BOM, LF line endings. Shopee only.

| Table | Columns | Notes |
|---|---|---|
| Stores (51) | Name, Shopee URL, Location, Total Products, Rating, Total Reviews, Followers, Chat Performance (`97%`), Joined Since (relative text: a number plus two words), Shop Id (Shopee shop id, 8-10 digits), Competitor For, Tag, Is Own Shop (`true`/`false`), Updated At, Created At | 3 own shops, 48 competitors. `Competitor For` is a comma-separated list of own shop names (valid on 42 of 42) |
| ProductListing (281) | SKU Title, Store, Latest Price, Minimal Qty, Grosir Summary, Sales / Month, Last Checked At, Total Sold, Total Reviews, Rating, Tag, Notes, Shopee URL, Display Name, Updated At, Created At | one row per product title per shop; 118 rows are own-shop listings. `Store` references Stores.Name (281 of 281). `Display Name` = `SKU Title - Store` |
| SalesLog (270) | Listing, Retail Price, Minimal Qty, Sales / Month, Checked Date, Display Name, Notes | `Listing` references ProductListing.Display Name (270 of 270). One row per listing in this export, 26 distinct dates: designed as a time series but currently holding one observation per listing |

**Wholesale tiers** are recorded only here, in `Grosir Summary`, as a formatted string: `1 - 9: Rp 5000; 10 - 99: Rp 4500; 100 - ∞: Rp 4000`. One to four tiers (3 tiers on 101 of 132 filled rows), an infinity sign for the open upper bound. 106 of the 132 are on own-shop listings. It reads like a computed summary of a tier table that was not exported.

Joins to BigSeller are weak: `SKU Title` is a mixed-case product name with no code; it matches a BigSeller Merchant SKU title case-insensitively on 17 of 281 rows and the family name part on 34 of 157 distinct titles. The three own shops can be matched to BigSeller stores by name. 218 of 281 listing URLs contain a Shopee shop id and item id, which could be joined to BigSeller's listing `Product ID`.

Format hazards: numbers carry thousands separators (`12,345`), inconsistently (`Sales / Month` has them in SalesLog but not in ProductListing); `Created At` is `DD-MM-YYYY` while `Updated At` and `Last Checked At` are `YYYY-MM-DD`; `Latest Price` is `0` on 12 rows (not yet checked).

### 8.3 Answer to the ticket's question

| Topic | Where it is recorded today |
|---|---|
| Purchase orders | BigSeller only (`purchase-order.xlsx`), entered at receiving time, with fees left at zero |
| Supplier links | nowhere per SKU; per PO as a free-text supplier name in BigSeller, often generic |
| Wholesale tiers | Shopee single tier (price + quantity) in the Google Sheet; multi-tier as text in Grist; live Shopee tier prices (without quantities) in the Shopee listing export's `activity information` |
| Per-channel prices | Google Sheet: Shopee retail entered by hand, TikTok and Lazada derived by multiplier. Actual live prices per listing variation are in the listing exports |
| Marketplace fees | Google Sheet: percentage rates per product row by category, plus a small per-marketplace table of fixed and percentage fees. Actual charged fees are in the order export for Shopee only |

## 9. JSON vs Excel

Only in the JSON captures:

- Numeric ids for everything: store, Merchant SKU, SKU x warehouse row, warehouse, package, order line, PO, listing, variation.
- Per order line: marketplace `platformItemId` **and `platformVariationId`**, the listing's parent SKU, the store SKU and the Merchant SKU side by side.
- **Bundle composition**: component SKU, pieces per bundle and cost allocation ratio (`orderItemList[].varSkuGroupVoList[]`). 7 of 582 lines are bundles, all single-component multi-packs of 20.
- Order type flags (`platformOrderType`, `hasDeducted`, `packageIndex`) that distinguish hub-stock rows from buyer orders.
- Manual orders (18) with their clear customer names, free-text courier and attachments. The CSV samples contain none.
- User marks as structured labels with ids; print and pick-list timestamps; pickup codes.
- Merchant SKU: number of mapped listings, update time.
- Totals for sizing: about 10,000 Merchant SKUs, tens of thousands of packages in the captured view, a few thousand POs and a few thousand Shopee listings.
- The store list including Blibli and the pseudo-stores.

Only in the Excel / CSV exports:

- All location detail: city, district, town, post code, address, phone.
- Money detail: management fee, transaction fee, seller and marketplace discounts, shipping discounts, vouchers, original vs paid price per line.
- Line cost (`Commodity Cost`), reference price, shelf, weight and dimensions per line.
- The full lifecycle timeline (paid, packed, printed, shipped, transit, completed, cancelled) and cancellation fields.
- Marketplace category names per line.
- PO lines (SKU, unit price, ordered and received quantity) and supplier details.
- The SKU mapping bridge, shelf stock, the shelf list, purchase suggestions with sales windows and lead times, the customer list with phone numbers.
- Listing variation names and Shopee campaign text.

For an upload-based import the Excel exports are sufficient for orders, stock, POs and listings, with three gaps that only the JSON fills: **bundle composition**, the **variation id on order lines**, and the **store list**. Bundle composition can be reconstructed by exporting the same orders in both the "merchant SKU" and the "single SKU" layout and comparing `Merchant SKU` and `To Allocate/Deduct`, but a direct bundle export from BigSeller would be far simpler (section 11).

## 10. Data quality hazards for import

Encoding and file format:

- Order exports are **CSV with a UTF-8 BOM and CRLF**; multi-value cells in the per-package layout contain CRLF inside quotes and a trailing CRLF. A naive line splitter breaks them.
- Grist CSVs are UTF-8 without BOM, LF, and contain the infinity sign. The supplier list contains Chinese characters.
- BigSeller xlsx files have no default style (openpyxl warns); harmless.
- Sheet names vary (`sheet`, `sheet1`, `SKU`, `Single SKU`); select by position or by header signature, not by name.

Headers:

- BigSeller headers are English and single-row. `export-shelf.xlsx` uses **merged cells** in the data. The Google Sheet has a **two-row header with merged group cells and duplicate column names**.
- The same concept has different names per export: `SKU Name` / `Merchant SKU` / `Merchant SKU Name`; `Item ID` / `Product ID`; `Shelf` / `Shelf Name`; `Warehouse` / `Receiving Warehouse` / `Shipping Warehouse`; `Image URL` / `Image Link`. In the order export `SKU` means the store SKU, while in inventory exports `SKU Name` means the Merchant SKU.
- `customer-list.xlsx` has a trailing unnamed column.

Numbers:

- **Most numbers are stored as text**, with mixed typing inside one sheet (for example `On Hand` numeric but `Defective Stock` text in the inventory list).
- Decimal formats vary for the same kind of value: `0`, `0.0`, `0.00`, `0.00000`, `9000.0`, `2000`. Use decimal parsing, never float equality. Dot is the decimal separator everywhere in BigSeller; Grist uses comma as a thousands separator.
- Quantities arrive as floats in xlsx (`20.0`).
- `--` (and in one JSON field `-`) means null. Empty string also means null. Both occur in the same column across marketplaces.
- Zero often means "not set" (weight, dimensions, reference cost, reference price), not a real zero.
- Sizes are packed as `L*W*H` in one text cell in the PO export.
- Google Sheet percentages have floating-point noise; `COGS` is sometimes a formula.

Identifiers:

- Marketplace ids have up to 19 digits (TikTok product and variation ids, Shopee tracking numbers). They are text in every sample file, with no scientific notation and no precision loss, **as long as nobody opens and re-saves the CSV in Excel**, which would convert them. In the JSON, Shopee item and variation ids are JSON numbers (safe at 11-12 digits); TikTok ids are strings on order lines. Always carry ids as strings.
- Order numbers are alphanumeric for Shopee and manual orders.

Dates and time:

- At least six formats: `DD Mon YYYY HH:mm` (orders, listings, POs), `DD Mon YYYY` (dates), `YYYY-MM-DD HH:mm` (Merchant SKU creation), `YYYY-MM-DD HH:mm:ss` (customer list), `YYYY/MM/DD` (PO JSON), and in Grist both `DD-MM-YYYY` and `YYYY-MM-DD`. English month abbreviations.
- No export states a timezone and none has seconds (except the customer list). Values look like local business time (packing and printing fall in working hours; ship-by deadlines are 23:59).
- **JSON epoch-millisecond fields are local wall-clock time encoded as UTC.** Proof: `shippedTime` (ms) minus `timeoutSeconds` (a true epoch in seconds, 16:59:59 UTC = 23:59:59 at UTC+7) is exactly 25,200 seconds on 269 of 282 rows, and each `xTimeStr` equals the UTC rendering of its `xTime`. Converting those epochs as real instants would shift everything by 7 hours. Parse the strings as local time in the business's zone instead.

Content:

- Text fields are repurposed: PO `Note` holds marketplace order numbers and staff names; supplier `Contact Information` and `Phone Number` hold a category and a city. Do not validate them as their label suggests.
- Personal data sits in unexpected places: the account email in PO `Creator` / `Purchaser` and supplier `Owner`; staff names in PO notes; buyer chat text in `Buyer Message`.
- Province and city names are not normalised across marketplaces (section 6). One Lazada courier value has a trailing space.
- Category language differs: Indonesian in order exports and TikTok listings, English in Shopee and Lazada listing exports. The inventory category is unused.
- Order-level money repeated on each line row (section 1); Lazada repeats it per unit and also splits order-level discounts per unit.
- `Base Unit` is empty in the per-line order layout but filled in the per-package layout of the same orders.
- Fee columns are empty for TikTok and Lazada; settlement columns are empty for all.

## 11. Open questions for the owner

1. **Warehouses.** BigSeller shows one warehouse; shelf codes have two prefixes. Is the second warehouse the second shelf prefix, or is it not in BigSeller at all? Should stock and restocking be tracked per building?
2. **Blibli and the pseudo-stores.** Two Blibli stores exist, plus POS and messenger order channels. Are they active and in scope?
3. **A second order export for the same orders.** Please export one day of orders now and again in a week (same filter), and include at least one cancelled order, one returned order, one split package and one manual order. This is needed to see how rows change and how splits look.
4. **Which order layout will be uploaded?** Per line or per package, and with which SKU option. The per-line "merchant SKU" layout is the most convenient for us.
5. **Bundles.** Is there a BigSeller export of combination SKU definitions (bundle -> component, quantity)? Are all bundles multi-packs of one component, or do mixed bundles exist?
6. **SKU code convention.** Is `FAMILY-variant-variant` an intended, stable rule? What do the letter prefixes mean, and is the first segment always the family? Are there Merchant SKUs outside this rule among the roughly 10,000?
7. **Shopee hub rows.** Are the rows whose receiver is a Shopee distribution hub stock transfers to Shopee, and are the related buyer orders shipped from that stock? Should hub stock be counted as inventory?
8. **Fees for TikTok and Lazada.** The order export has fees only for Shopee and no settlement data. Is there another BigSeller export (settlement or profit report) that carries actual fees and payouts per order?
9. **Cost.** Which cost should drive pricing and profit: BigSeller's weighted average cost, or the COGS in the Google Sheet? Freight and import costs are not entered on POs; where are they tracked?
10. **Suppliers.** Most POs use a generic supplier with the source in the note. Is a proper supplier per SKU wanted, and can the owner supply that list? Is the BigSeller supplier list complete?
11. **PO timing.** POs seem to be created when goods arrive. Should the new system record a PO at ordering time, to track lead time and stock on the way?
12. **Pricing workbook scope.** The Google Sheet covers one product line and keys rows by family code only. Is there a similar workbook for the other lines? Can a full Merchant SKU column be added, or is pricing really per family + variant name?
13. **Wholesale tiers.** Grist shows up to four tiers as text, the Google Sheet one tier. Which is authoritative, and is there an underlying tier table in Grist that can be exported?
14. **Lazada location.** Lazada exports hide province and city. Is Lazada location analysis needed, and does the owner have another source (Lazada seller centre export)?
15. **Customer identity.** For manual orders, is the BigSeller customer list the source of truth? Should marketplace buyers be tracked as customers at all, given that only Shopee exposes a stable username?
16. **Export date.** Stock and listing exports carry no timestamp. Is it acceptable that the person uploading states the export date, or should the file name carry it?
