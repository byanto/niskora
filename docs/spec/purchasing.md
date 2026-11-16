# Purchasing: suppliers, supply sources, purchase orders and their documents

Who the business buys from, what a purchase order is, how received goods are matched to it, and the three documents it produces. Rule ids start with `PUR`.

Read [README.md](README.md) first, then [foundation.md](foundation.md) and [restock.md](restock.md). Terms are defined in [`GLOSSARY.md`](../../GLOSSARY.md) and are not redefined here.

## Check this

Points where the closed tickets disagree, or leave a gap that a rule below had to fill. The later ticket wins in each case. The owner confirms or corrects each one in the final review.

1. **One supplier name, and no check against BigSeller (PUR-2, PUR-3).** The purchase order decision gave a supplier a separate "name BigSeller knows it by", and the import test said that name is "checked against the BigSeller supplier list when it is entered" and that a supplier without it gets no import file. The later ticket on outside data says nothing about suppliers is imported and that the supplier's name itself must equal the name in BigSeller. PUR-2 follows the later ticket: one name, typed to match BigSeller. niskora then has no list to check it against, so a wrong name still creates a supplier in BigSeller without a warning. PUR-3 only reminds the user.
2. **Document language on the supplier (PUR-1).** The purchase order decision listed a document language per supplier. The later tickets let the buying channel decide the documents and list the supplier's fields without it. PUR-1 has no document language.
3. **Supplier city and phone (PUR-1).** The Daftar Pesanan prints the supplier's city and the forwarder invoice an optional phone number. The later list of supplier fields has only "contact". PUR-1 keeps city and phone as optional fields beside a free contact note.
4. **Uploading a photo by hand (PUR-80).** The purchase order decision let a user upload a photo where one is missing. The later ticket says there is no photo upload by hand in v1. PUR-80 follows the later ticket: a neutral placeholder is printed.
5. **Where the product link and the Chinese item name live (PUR-9, PUR-10).** The purchase order decision kept a 1688 link per SPU and supplier. The later ticket puts a product link on each supply source. PUR-9 follows the later ticket. The Chinese item name, units per carton and carton size stay per SPU and supplier (PUR-10), and the sheet reads all of them from SPU rows only.
6. **The forwarder warehouse belongs to a 1688 purchase (PUR-27).** The layout decision says "a purchase order for a Chinese supplier names the forwarder warehouse". The later ticket says the buying channel is the one thing that marks a 1688 purchase. PUR-27 asks for a forwarder warehouse on a purchase order whose buying channel is 1688.
7. **A 1688 purchase from a supplier whose currency is rupiah (PUR-81).** The forwarder invoice prints prices in yuan. Buying channel and currency are separate fields, so the pair "1688 and rupiah" is possible. PUR-81 offers the forwarder invoice only when the supplier's currency is yuan.
8. **An empty Unit Price in the BigSeller import file (PUR-85).** The file leaves Unit Price empty for a yuan supplier and for a resend line. The import test used a unit price on every row, so BigSeller accepting an empty one is not proven. This must be tried on the first real file.
9. **Which lead time gives the expected arrival (PUR-34).** "The placed date plus lead time" does not say whose lead time when lines have their own. PUR-34 uses the supplier's lead time and lets the user change the date.
10. **A line cut to zero (PUR-39).** A placed line can be set to 0 when the supplier cannot deliver it. No ticket says how such a line closes. PUR-39 treats it as closed, with no shortfall, so the purchase order can still become Completed.
11. **"Round up to full cartons" needs the sales rate (PUR-48).** The extra units are spread "in proportion to their sales rate". `restock` depends on `purchasing`, so `purchasing` cannot ask `restock` for it. PUR-48 uses the sales rate frozen on each line made from restock advice, and spreads evenly over lines that have none. This is a reading; another way is for `restock` to do the rounding and hand the result to `purchasing`.
12. **Adding a Single SKU with no supply source (PUR-24).** The purchase order decision says this creates the supply source. With buying channels, PUR-24 creates it on the purchase order's buying channel.
13. **The 1688 order number (PUR-28).** It was to head the forwarder invoice and is no longer printed anywhere. PUR-28 keeps it as an optional field shown on screen only.
14. **A BigSeller document that covers two purchase orders (PUR-56).** The tickets say one purchase order can be matched to several BigSeller documents. They do not say whether one BigSeller document can be split over several purchase orders. PUR-56 allows it, since the match is confirmed per Single SKU.

## 1. Suppliers

- **PUR-1** niskora keeps the list of [suppliers](../../GLOSSARY.md#purchasing). A supplier has a name, a currency (rupiah or yuan), a [lead time](../../GLOSSARY.md#purchasing) and an [order cycle](../../GLOSSARY.md#purchasing) in days, a usual [buying channel](../../GLOSSARY.md#purchasing), and optionally a city, a phone number, a contact note and one link (a Shopee store or a 1688 shop).
- **PUR-2** Suppliers are typed by hand. Nothing about suppliers is imported from BigSeller, Google Sheets or Grist.
- **PUR-3** The supplier's name must equal the name BigSeller has for it. BigSeller matches the name without regard to letter case, and creates a new supplier without any warning when it does not know the name. The supplier screen says so beside the name field.
- **PUR-4** A party the business buys from in two ways is one supplier. The usual buying channel is only the default used when none is stated.
- **PUR-5** The supplier link shows on screen only and never on a document.
- **PUR-6** niskora shows each supplier's real average number of days from Placed to the first stock-in beside the lead time a user entered. It never changes the lead time by itself.
- **PUR-7** niskora shows per supplier the units it owes ([To be resent](../../GLOSSARY.md#purchasing)), the unresolved [shortfalls](../../GLOSSARY.md#purchasing) and the total written off as Loss.

_Sources: [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20), [Test the BigSeller purchase order import with a small file](https://github.com/byanto/niskora/issues/21), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 2. Buying channels and supply sources

- **PUR-8** The buying channels are a fixed list: Shopee (the supplier's Shopee store), WhatsApp, 1688 and Other.
- **PUR-9** A [supply source](../../GLOSSARY.md#purchasing) is one supplier, one [Single SKU](../../GLOSSARY.md#catalogue) and one buying channel. It holds the [goods price](../../GLOSSARY.md#purchasing) in the supplier's currency and, all optional, a product link, an order multiple, a minimum quantity and its own lead time. The same supplier can have two supply sources for one Single SKU, one per buying channel, at different prices.
- **PUR-10** Per [SPU](../../GLOSSARY.md#catalogue) and supplier niskora keeps, all optional: the Chinese item name, the units per [carton](../../GLOSSARY.md#purchasing) and the carton's length, width and height. A carton may mix the SPU's Merchant SKUs.
- **PUR-11** A [Combination SKU](../../GLOSSARY.md#catalogue) never has a supply source.
- **PUR-12** One supply source of a Single SKU is its [preferred supply source](../../GLOSSARY.md#purchasing). The first one created becomes preferred and stays so until a user, or a sheet row with "preferred = yes", moves it.
- **PUR-13** The supply source price for the Shopee buying channel is the listed price before any voucher. Vouchers and delivery cost are not recorded.
- **PUR-14** The product link shows on screen only and never on a document.
- **PUR-15** Supply sources are edited on screen, one at a time, and removed on screen only.
- **PUR-16** A Single SKU added by an import inherits its SPU's supply sources when every existing Single SKU of the SPU has the same ones (the same suppliers and buying channels). Otherwise it has none. How `purchasing` hears of the new Single SKU is in [engineering.md](engineering.md) (ENG-15).
- **PUR-17** Assigning a supplier to ticked SPUs in the "No supplier" block of restock advice creates one supply source per Single SKU of those SPUs, on the supplier's usual buying channel.
- **PUR-18** Every change to a supplier or a supply source goes to the audit trail: who, when, the old value and the new one.

_Sources: [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 3. The supply source sheet

- **PUR-19** The [supply source sheet](../../GLOSSARY.md#purchasing) is niskora's own xlsx format, owned by `purchasing`. It is downloaded and uploaded from the supply sources screen and never from the BigSeller upload page.
- **PUR-20** One row names an SPU and a supplier and applies to every Single SKU of that SPU. A row that names a Merchant SKU applies to that one and wins over the SPU row.
- **PUR-21** Columns:

  | Column | Required | Note |
  | --- | --- | --- |
  | SPU code or Merchant SKU code | yes | one of the two |
  | Supplier name | yes | must be a supplier niskora already has |
  | Buying channel | no | empty means the supplier's usual one |
  | Goods price | no | in the supplier's currency |
  | Preferred | no | "yes" moves the preferred supply source |
  | Order multiple | no | |
  | Minimum quantity | no | |
  | Lead time | no | overrides the supplier's |
  | Units per carton | no | read from SPU rows only |
  | Carton length, width, height | no | read from SPU rows only |
  | Chinese item name | no | read from SPU rows only |
  | Product link | no | |

- **PUR-22** An upload adds and updates and never deletes. An empty cell never clears a stored value, and a missing row never removes a supply source. Every change goes to the audit trail together with its upload.
- **PUR-23** Any faulty row rejects the whole file: an unknown supplier, an unknown code, a Combination SKU or a number that cannot be read. The answer lists every faulty row with its reason. An unknown supplier name never creates a supplier.

_Sources: [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 4. The purchase order

- **PUR-24** A [purchase order](../../GLOSSARY.md#purchasing) is for one supplier through one buying channel. It holds Single SKUs only, and Combination SKUs are refused. Merchant SKUs of different companies may be mixed. Adding a Single SKU that has no supply source for that supplier and buying channel creates it.
- **PUR-25** A purchase order is made from one supplier's restock advice ([restock.md](restock.md), RST-42) or by hand.
- **PUR-26** Its number is niskora's own running number per year with no separators, in the form `NK` + year + four digits, such as `NK20260001`. It is given when the Draft is created and is never reused.
- **PUR-27** The buying channel comes from the supply sources of the lines and can be changed while the purchase order is a Draft. Changing it reads each line's goods price and product link again from the supply source of the new buying channel. A line with no supply source for that buying channel keeps its price and is flagged. A purchase order on the 1688 buying channel also names the [forwarder warehouse](../../GLOSSARY.md#purchasing) the goods go to.
- **PUR-28** Optional fields: a free note, the local courier and tracking number (the Chinese courier that carries the goods to the forwarder), and the 1688 order number, which shows on screen only.
- **PUR-29** A [purchase order line](../../GLOSSARY.md#purchasing) is one Single SKU with its ordered quantity and goods price. Lines are shown grouped by SPU. A line made from restock advice keeps the frozen working behind it (RST-43).
- **PUR-30** The goods price of a new line comes from the supply source and can be changed on the line. Every line except a [resend line](../../GLOSSARY.md#purchasing) needs a goods price before the purchase order is placed, and the price can still be changed afterwards.
- **PUR-31** When the purchase order is placed, each line's goods price is written back to its supply source. There is no price history table: past purchase orders are the history.
- **PUR-32** A line holds the goods price only, in the supplier's currency. niskora stores no exchange rate and works out no landed cost. Once a line is received, niskora shows the stock-in price from the movement beside the goods price.

_Sources: [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28), [Reporting and read model approach](https://github.com/byanto/niskora/issues/15)._

## 5. States and changes

- **PUR-33** The states are Draft, [Placed](../../GLOSSARY.md#purchasing), Completed and Cancelled. A Draft counts for nothing. "Partly received" is derived from the lines and is not a state.
- **PUR-34** A user marks a Draft as Placed after the supplier has been asked. [Expected arrival](../../GLOSSARY.md#purchasing) is then the placed date plus the supplier's lead time, and a user can change it.
- **PUR-35** A Placed purchase order with open lines past its expected arrival is Overdue.
- **PUR-36** A Placed purchase order can still change until a line is received: a quantity lowered or raised, a line set to 0, a line added or removed. This is how the supplier's own changes (out of stock, another variant offered) are recorded. Payment and shortfalls follow the current quantities.
- **PUR-37** Every change to a Placed purchase order is kept in the audit trail: who, when, the old value and the new one. There are no numbered revisions.
- **PUR-38** A line is open from the moment the purchase order is placed. It becomes a [closed line](../../GLOSSARY.md#purchasing) on its first [Purchase Stock-In](../../GLOSSARY.md#stock) whatever the count, unless a user has marked it "rest still coming", or when a user closes it by hand.
- **PUR-39** A line whose quantity is 0 expects nothing. It is closed and has no shortfall.
- **PUR-40** The purchase order is Completed when every line is closed.
- **PUR-41** A purchase order can be Cancelled only before any stock-in. After that, the lines that will not arrive are closed by hand.
- **PUR-42** `purchasing` answers [on order](../../GLOSSARY.md#purchasing) per Single SKU: the units on the open lines of Placed purchase orders, resend lines included, as [restock.md](restock.md) reads it (RST-24, RST-25).

_Sources: [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20), [Restock calculation rules](https://github.com/byanto/niskora/issues/8)._

## 6. Cartons and volume

- **PUR-43** A Draft shows per SPU the number of cartons and the volume in cubic metres, and one total volume for the purchase order, so that freight can be estimated before ordering.
- **PUR-44** An SPU with no carton data shows no carton count and no volume, and the total says it is incomplete. Carton data is never required.
- **PUR-45** Whole cartons are a wish and not a rule. Restock advice rounds only by order multiple and minimum quantity.
- **PUR-46** The Draft marks each SPU whose total is not a whole number of cartons and offers "round up to full cartons".
- **PUR-47** A supplier that sells only by the carton is handled with the order multiple.
- **PUR-48** "Round up to full cartons" spreads the extra units over the SPU's lines in proportion to their sales rate, taken from the working frozen on each line. Lines with no frozen working share the extra units evenly.

_Sources: [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 7. Receiving

The flow, end to end:

1. Goods arrive. The Daftar Pesanan is printed again and the unloading team writes the counts on it.
2. The count is entered once, in BigSeller, and the BigSeller purchase document is submitted.
3. The next stock movement import brings the Purchase Stock-In rows.
4. niskora matches them to purchase orders, fills the received quantities, closes lines and shows shortfalls and overages.

- **PUR-49** The [received quantity](../../GLOSSARY.md#purchasing) is read from Purchase Stock-In movements and is never typed into niskora ([ADR 0004](../adr/0004-received-quantities-come-from-bigseller-stock-movements.md)).
- **PUR-50** BigSeller's own purchase document, with its `PO...` number, is not a purchase order in niskora. It appears only as the document number on Purchase Stock-In movements.
- **PUR-51** The niskora number written in the note of the BigSeller import file does not reach the stock movements, so it cannot link a stock-in to its purchase order. The match below is the only link.
- **PUR-52** When Purchase Stock-In rows arrive under a `PO...` number niskora has not seen, it proposes the Placed purchase order whose open lines hold those Single SKUs. A user confirms the match. Nothing is matched without that confirmation.
- **PUR-53** Until a user confirms, and for goods bought outside niskora, the rows are [unmatched stock-ins](../../GLOSSARY.md#purchasing). An unmatched stock-in can be dismissed. Unmatched stock-ins are counted under "needs attention".
- **PUR-54** A confirmed match fills the received quantity of each line from the rows for its Single SKU and closes the line (PUR-38).
- **PUR-55** A line marked "rest still coming" stays open, and later stock-ins add to its received quantity until a user closes it.
- **PUR-56** One purchase order can be matched to several BigSeller documents, and one BigSeller document to several purchase orders.

_Sources: [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Test the BigSeller purchase order import with a small file](https://github.com/byanto/niskora/issues/21), [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20)._

## 8. Shortfalls, overages and resend lines

- **PUR-57** A closed line whose received quantity is below its current ordered quantity has a shortfall. It is Unresolved until a user gives it one outcome: Refunded, Loss or To be resent.
- **PUR-58** A shortfall is valued at the line's goods price. niskora shows unresolved shortfalls per supplier and a total of losses. Refund money is not tracked.
- **PUR-59** An [overage](../../GLOSSARY.md#purchasing) is recorded as a fact with no follow-up.
- **PUR-60** With To be resent the line stays closed and its units do not count as on order. niskora keeps the owed units per supplier and shows them on the supplier and with that supplier's restock advice.
- **PUR-61** When the next purchase order for that supplier is started, niskora offers the owed units as resend lines. A resend line has no goods price, names the earlier purchase order and counts as on order like any other line.
- **PUR-62** The arrival of a resend line settles the earlier shortfall. If it comes short again, the resend line has a shortfall of its own with its own outcome.
- **PUR-63** Owed units can be turned into Refunded or Loss at any time before they are resent.

_Sources: [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20)._

## 9. Forwarders

- **PUR-64** niskora keeps a list of [forwarders](../../GLOSSARY.md#purchasing). Each has one [marking code](../../GLOSSARY.md#purchasing) and one or more forwarder warehouses, each with its address.
- **PUR-65** Forwarder freight prices are not kept in niskora.

_Sources: [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20)._

## 10. Documents in general

- **PUR-66** A purchase order produces up to three documents: the [Daftar Pesanan](../../GLOSSARY.md#purchasing), the [forwarder invoice](../../GLOSSARY.md#purchasing) and the BigSeller import file.
- **PUR-67** The buying channel decides which are offered, and nothing else does:

  | Buying channel | Daftar Pesanan for the supplier | Daftar Pesanan for unloading | Forwarder invoice | BigSeller import file |
  | --- | --- | --- | --- | --- |
  | WhatsApp, Other | yes | yes | no | yes |
  | Shopee | no | yes | no | yes |
  | 1688 | no | yes | yes | yes |

- **PUR-68** The Daftar Pesanan is one document. The copy for the supplier and the copy for unloading are the same print, made at different times.
- **PUR-69** The Daftar Pesanan and the forwarder invoice are PDF files from fixed templates in their own language, separate from the English app.
- **PUR-70** No company name and no store name ever appears on a purchase order document. The Daftar Pesanan is headed by one [trading name](../../GLOSSARY.md#purchasing), set once in Setup.
- **PUR-71** Every document is produced from the purchase order's current lines and can be produced again at any time.
- **PUR-72** niskora never sends a document to anyone. A person downloads it and sends or prints it.
- **PUR-73** niskora produces no packing list.

_Sources: [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28), [Domain model and glossary](https://github.com/byanto/niskora/issues/5)._

## 11. The Daftar Pesanan

In Bahasa Indonesia, A4 portrait.

- **PUR-74** Layout:

  | Part | Content |
  | --- | --- |
  | Header, first page | Left: the trading name, then "Kepada" with the supplier's name and city. Right: the title DAFTAR PESANAN, "No. Pesanan" and "Tanggal". |
  | Header, later pages | One line: the trading name, the number, the supplier and "(lanjutan)". |
  | Body | One continuous ruled table with the columns Foto, Varian, Jumlah, Diterima, Catatan. |
  | SPU title row | Grey, with the SPU code, the SPU name and "Total n pcs". |
  | Photo | 32 mm, beside the SPU's rows. |
  | Row | One per Merchant SKU: the variant name, the ordered quantity, an empty box for the counted quantity and an empty Catatan cell with no dotted line. |
  | End | Total units with the number of SPUs and variants, and two signature lines with name and date: "Dipesan oleh" and "Diterima dan dihitung oleh". |
  | Footer | Left: the number and the date. Right: "Halaman n dari m". |

- **PUR-75** Never printed: prices, subtotals or totals in money, and Merchant SKU codes. The supplier quotes the price, and the unloading team must not see cost.
- **PUR-76** Page breaks: pages are filled. An SPU that does not fit continues on the next page with its title row and photo repeated and "(lanjutan)". A stub of one or two rows is never left behind.
- **PUR-77** A reprint shows the current quantities only, with no trace of earlier ones. A line whose quantity is 0 is left out, and an SPU with no lines left disappears. The header adds "Dicetak" with the print date, so that two prints can be told apart.
- **PUR-78** For a 1688 purchase the header adds the local courier and its tracking number when they are known. No forwarder and no marking code is printed.
- **PUR-79** A resend line is printed like any other line, marked "kirim ulang dari" with the earlier purchase order number.
- **PUR-80** Photos are the ones the catalogue keeps (FND-25 to FND-28). Where there is none, the neutral placeholder is printed. The unloading team sees the ordered quantity while counting.

_Sources: [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20), [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 12. The forwarder invoice

In Chinese, A4 portrait, a classic table. It is given to the forwarder for customs only.

- **PUR-81** It is offered for a purchase order on the 1688 buying channel whose supplier's currency is yuan. There is one per purchase order.
- **PUR-82** Layout:

  | Part | Content |
  | --- | --- |
  | Header, first page, left | The marking code (唛头) in large type in place of a trading name, then the forwarder warehouse and its address (收货仓库), then the supplier's name with the optional phone number (供应商, 电话). |
  | Header, first page, right | The title 发票 with INVOICE underneath, the local courier (物流公司) and the tracking number (运单号码). Both are printed blank when not yet known. |
  | Header, later pages | One line: the marking code and the tracking number. |
  | Body | Columns 序号, 图片 (a 17 mm photo), 品名, 数量 (件), 单价 (元), 金额 (元). |
  | End | The number of rows (共 n 项), the total quantity (总数量) and the total amount (合计金额). |
  | Footer | The marking code and 第 n 页 / 共 m 页. |

- **PUR-83** There is one row per Chinese item name and goods price: an SPU whose Merchant SKUs differ in price gives one row per price. A row shows the total quantity of its lines. There are no variants, no SPU code and no carton columns.
- **PUR-84** Never printed: the niskora purchase order number, the 1688 order number, the date, the currency and the trading name. When the purchase order changes, the forwarder invoice is exported again.

_Sources: [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20), [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 13. The BigSeller import file

- **PUR-85** From a Placed purchase order niskora offers a file in BigSeller's own purchase order import template. BigSeller turns it into a draft purchase document, which the owner completes and later submits when the goods arrive. One row per line:

  | Column | Value |
  | --- | --- |
  | Provisional No. | the same value on every row, so the rows become one BigSeller document |
  | SKU | the Merchant SKU code |
  | Quantity | the current ordered quantity |
  | Unit Price | the goods price when the supplier's currency is rupiah; empty when it is yuan, and empty for a resend line |
  | Currency | always `IDR` |
  | Supplier | the supplier's name |
  | Estimated Arrival Date | the expected arrival, as a real date cell |
  | Note | the niskora purchase order number |
  | Exchange Rate and every fee column | empty |

  The column headers themselves are those of BigSeller's template and are taken from it when the file is built.

- **PUR-86** BigSeller only ever sees rupiah in v1. For a rupiah supplier the Unit Price is a starting value, and the owner overwrites it in BigSeller with the final cost, goods price plus delivery. For a yuan supplier the owner types the final rupiah cost there. niskora keeps yuan prices for the forwarder invoice.
- **PUR-87** Every row of one file carries the same supplier, date and note. A line whose quantity is 0 is left out.
- **PUR-88** The file can be downloaded again and always reflects the current lines.
- **PUR-89** The stock-in price the owner enters in BigSeller returns to niskora inside the [Weighted Average Cost](../../GLOSSARY.md#cost) and on the Purchase Stock-In movement.

_Sources: [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Test the BigSeller purchase order import with a small file](https://github.com/byanto/niskora/issues/21), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 14. The 1688 flow

A 1688 purchase is an ordinary purchase order. This is the same set of rules in the order a person meets them.

1. A Draft is made from restock advice, on the 1688 buying channel, and names a forwarder warehouse.
2. The order is placed by hand on the 1688 website. niskora is not involved.
3. The purchase order is marked Placed, with the 1688 order number if wanted.
4. The forwarder invoice is exported and given to the forwarder. The courier and tracking number are typed in when known, and the invoice is exported again.
5. On arrival the Daftar Pesanan is printed for unloading, and receiving follows section 7.

- **PUR-90** Nothing in niskora treats a 1688 purchase differently except the documents offered (PUR-67) and the forwarder warehouse (PUR-27).

_Sources: [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._
