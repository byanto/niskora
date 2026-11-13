# niskora

The business brain that sits beside BigSeller for a multi-store Indonesian e-commerce seller. It turns BigSeller exports into a trusted catalogue, order history and stock history, and builds restock advice, purchase orders and analysis on top.

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
A package kept out of the order history because one of its lines names a SKU the catalogue does not know yet.
_Avoid_: Failed row, quarantined order, rejected package

**Not seen**:
The state of a package or SKU that a later export should have contained but did not. It is reported and never treated as a deletion.
_Avoid_: Missing, deleted, dropped

**Stale**:
A data set whose last successful import is older than its allowed age.
_Avoid_: Outdated, expired

**Business day**:
Midnight to midnight in Jakarta time (WIB).
_Avoid_: Day, calendar day

### Catalogue

**SKU**:
A sellable item identified by its Merchant SKU code, which is the key that joins every export.
_Avoid_: Product, item, variant, Merchant SKU

**Retired SKU**:
A SKU that is no longer in BigSeller's SKU list but is kept with all its history.
_Avoid_: Deleted SKU, inactive SKU, archived SKU

**Bundle**:
A SKU that is sold as one item and stands for fixed quantities of other SKUs. It holds no stock of its own.
_Avoid_: Combination SKU, kit, multi-pack

**Component**:
A SKU that a bundle is made of.
_Avoid_: Child SKU, single SKU

### Orders

**Package**:
One parcel of an order, identified by its Package No. It is the unit an order export is imported by.
_Avoid_: Shipment, parcel, order

**Hub package**:
A package that moves stock to a Shopee hub for an order Shopee fulfils. It mirrors the buyer's package and is never a customer order.
_Avoid_: Matched package, FBS order, hub order

**Final status**:
A package status that will not change again: Completed, Canceled, or Return & Refund.
_Avoid_: Closed, done, terminal status

### Stock

**Stock snapshot**:
The stock figures of every SKU as BigSeller reported them on one business day.
_Avoid_: Inventory list, stock level, balance

**Stock movement**:
One recorded change to a SKU's stock, with the stock before and after, its type and the document that caused it.
_Avoid_: Stock transaction, stock in/out record, inventory log

**Stock history gap**:
A break in a SKU's chain of stock movements, where one movement's stock after does not match the next one's stock before.
_Avoid_: Missing movement, hole
