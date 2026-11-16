# Restock: restock advice

How niskora decides what to reorder, when and how much. Rule ids start with `RST`.

Read [README.md](README.md) first, then [foundation.md](foundation.md), which defines [demand](../../GLOSSARY.md#orders), stock and freshness. Terms are defined in [`GLOSSARY.md`](../../GLOSSARY.md) and are not redefined here.

## Check this

Points where the closed tickets disagree, or leave a gap that a rule below had to fill. The later ticket wins in each case. The owner confirms or corrects each one in the final review.

1. **The default order cycle (RST-22).** The restock decision gave a Single SKU with no supply source a default lead time of 14 days and a default order cycle of 30 days. The later ticket on outside data changed the default lead time to 60 days and did not mention the order cycle. RST-22 keeps a default order cycle of 30 days, changeable in Setup.
2. **Which lead time and order cycle a Single SKU uses (RST-20, RST-21).** The tickets say the preferred supply source "decides the lead time". RST-20 spells out the order: the preferred supply source's own lead time, else its supplier's. The order cycle always comes from that supplier.
3. **On order for a line marked "rest still coming" (RST-25).** No ticket says how many units such a line still counts for after a first delivery. RST-25 counts the ordered quantity minus the received quantity, and never less than zero.
4. **Fewer than 90 countable days (RST-9).** The sales rate is "over the most recent 90 countable days, looking back at most 365 days", and a Single SKU with fewer than 14 gets no advice. For 14 to 89 countable days, RST-9 divides by the countable days there are.
5. **Whole units and the order of rounding (RST-17).** The formula can give a fraction. RST-17 rounds up to a whole unit, then raises to the minimum quantity, then rounds up to the order multiple, so the result always satisfies both. The tickets name the two steps without an order.
6. **What the navigation count counts (RST-39).** "A count of SKUs needing action" is read as the Single SKUs at Late or Order now. Top-ups at Fine are not counted.
7. **Filtering by buying channel (RST-37).** A Single SKU can have supply sources on several buying channels. RST-37 filters on the buying channel of its preferred supply source. Single SKUs with no supply source have no buying channel and show only when the filter is off.
8. **Level and order of the "No supplier" block (RST-33).** The block uses the default lead time for the level and the sum. With the default at 60 days, many of these Single SKUs will show as Late or Order now until they are given a supplier. Whether they are included in the navigation count is not settled. RST-39 includes them.
9. **Owed units in the sum (RST-27).** Units a supplier owes as "To be resent" do not count as on order until they are on a purchase order. They are shown beside the supplier's advice, and the suggested quantity is not lowered for them.

## 1. What restock advice is

- **RST-1** [Restock advice](../../GLOSSARY.md#restock) is worked out per [Single SKU](../../GLOSSARY.md#catalogue): whether to reorder now, and how many units.
- **RST-2** The numbers are worked out in code, never by a language model. The [agent](../../GLOSSARY.md#agent) can read and explain restock advice and changes nothing ([agent.md](agent.md)).
- **RST-3** v1 has no forecast. The [sales rate](../../GLOSSARY.md#restock) sits behind one seam in the `restock` module, so that a statistical forecast can replace it later without changing anything else.
- **RST-4** Advice stays inside the app. v1 sends no email and no WhatsApp message.

_Sources: [Restock calculation rules](https://github.com/byanto/niskora/issues/8)._

## 2. Demand, countable days and the sales rate

- **RST-5** The sales rate is built on demand as [foundation.md](foundation.md) defines it (FND-51 to FND-53): every package that is not Canceled, not an [abandoned package](../../GLOSSARY.md#orders) and not a [hub package](../../GLOSSARY.md#orders), on the business day of its Order Time, with [Combination SKUs](../../GLOSSARY.md#catalogue) counted through their [component lines](../../GLOSSARY.md#orders) and [unmapped lines](../../GLOSSARY.md#orders) left out. `restock` never counts demand by itself.
- **RST-6** A [stockout day](../../GLOSSARY.md#restock) is a business day a Single SKU began with zero [on hand](../../GLOSSARY.md#stock), as `stock` answers it from the movement chain (FND-76). A day on which on hand is unknown, inside a [stock history gap](../../GLOSSARY.md#stock) or before the stock history starts (FND-77), is treated as a stockout day.
- **RST-7** A [campaign day](../../GLOSSARY.md#restock) is a business day a user has marked as a sales event, such as 11.11, 12.12 or a payday sale. Any user maintains the list. niskora never detects a spike by itself.
- **RST-8** A [countable day](../../GLOSSARY.md#restock) is a business day that is neither a stockout day for the Single SKU nor a campaign day. A day that is not countable is left out together with its demand.
- **RST-9** The sales rate is demand per day over the Single SKU's most recent 90 countable days, looking back at most 365 days. When the 365 days hold fewer than 90 countable days, the rate is taken over those there are.
- **RST-10** With fewer than 14 countable days in the 365 days, the Single SKU shows "not enough history" and gets no quantity. This includes a new Single SKU.
- **RST-11** A rate over the most recent 30 countable days is shown beside the sales rate as a trend signal only. It is never used in the sum.
- **RST-12** Units sitting at a Shopee hub are ignored (FND-48). They are neither stock nor demand until a buyer orders.

_Sources: [Restock calculation rules](https://github.com/byanto/niskora/issues/8), [Import rule changes from the full history](https://github.com/byanto/niskora/issues/23), [Hub package and final status rules after the second order export](https://github.com/byanto/niskora/issues/24)._

## 3. The formula

For one Single SKU, with every figure in units or days:

- **RST-13** Reorder when: [available](../../GLOSSARY.md#stock) + [on order](../../GLOSSARY.md#purchasing) < sales rate x ([lead time](../../GLOSSARY.md#purchasing) + [safety days](../../GLOSSARY.md#purchasing)).
- **RST-14** Suggested quantity = sales rate x (lead time + [order cycle](../../GLOSSARY.md#purchasing) + safety days) - (available + on order).
- **RST-15** Available is used, not on hand, because [allocated](../../GLOSSARY.md#stock) units are demand already counted. It comes from the newest [inventory snapshot](../../GLOSSARY.md#stock). It can be negative and is used as it is.
- **RST-16** A suggested quantity of zero or less means nothing is suggested.
- **RST-17** A suggested quantity above zero is rounded up to a whole unit, raised to the preferred supply source's minimum quantity, then rounded up to its order multiple. Both are optional, and many Single SKUs have neither. Whole [cartons](../../GLOSSARY.md#purchasing) play no part here; they are offered on the purchase order ([purchasing.md](purchasing.md)).
- **RST-18** Safety days is one setting for the whole business. It starts at 7 and is changed in Setup.

_Sources: [Restock calculation rules](https://github.com/byanto/niskora/issues/8), [Purchase order model and documents](https://github.com/byanto/niskora/issues/9)._

## 4. Lead time, order cycle and supply sources

- **RST-19** A Single SKU with several [supply sources](../../GLOSSARY.md#purchasing) has one [preferred supply source](../../GLOSSARY.md#purchasing). It decides the lead time, the rounding and which [supplier](../../GLOSSARY.md#purchasing) the Single SKU is listed under. There is no logic that splits a quantity over suppliers. A user may pick another supplier when making the purchase order.
- **RST-20** The lead time is the preferred supply source's own lead time when it has one, and otherwise its supplier's.
- **RST-21** The order cycle is that supplier's.
- **RST-22** A Single SKU with no supply source uses a default lead time and a default order cycle from Setup. The default lead time starts at 60 days and the default order cycle at 30 days.
- **RST-23** The [buying channel](../../GLOSSARY.md#purchasing) changes nothing in the sum.

_Sources: [Restock calculation rules](https://github.com/byanto/niskora/issues/8), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 5. On order

- **RST-24** On order is niskora's own figure: the units on the open lines of [Placed](../../GLOSSARY.md#purchasing) purchase orders, [resend lines](../../GLOSSARY.md#purchasing) included. BigSeller's [on the way](../../GLOSSARY.md#stock) figure is never used. A Draft purchase order counts for nothing.
- **RST-25** A line stops counting once it is a [closed line](../../GLOSSARY.md#purchasing), whatever was counted on arrival: a short delivery is usually final. A line a user has marked "rest still coming" stays open and counts for its ordered quantity minus its [received quantity](../../GLOSSARY.md#purchasing).
- **RST-26** An [overage](../../GLOSSARY.md#purchasing) needs no rule: the extra units are on-hand stock.
- **RST-27** Units owed by a supplier under a shortfall that is [To be resent](../../GLOSSARY.md#purchasing) do not count as on order until they are put on a purchase order as a resend line. The owed units are shown with that supplier's restock advice.
- **RST-28** `purchasing` owns the on order figure and answers it per Single SKU. `restock` asks for it and never reads purchase orders.

_Sources: [Restock calculation rules](https://github.com/byanto/niskora/issues/8), [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20)._

## 6. Who gets advice

- **RST-29** These get no advice: a Single SKU with a sales rate of zero, a Single SKU with not enough history, a [Discontinued Merchant SKU](../../GLOSSARY.md#catalogue), a [Retired Merchant SKU](../../GLOSSARY.md#catalogue) and a Combination SKU.
- **RST-30** niskora can say for any [Merchant SKU](../../GLOSSARY.md#catalogue) why it gets no advice.
- **RST-31** A Combination SKU's sales reach its [components](../../GLOSSARY.md#catalogue) through demand, so the components get the advice.
- **RST-32** A Single SKU with no supply source still gets advice. It is shown in a "No supplier" block and counted under "needs attention".
- **RST-33** The "No supplier" block uses the defaults of RST-22 for the sum and the level.
- **RST-34** In the "No supplier" block a user can tick SPUs and assign a supplier. This creates supply sources on the supplier's usual buying channel ([purchasing.md](purchasing.md)). No purchase order can be made from this block.

_Sources: [Restock calculation rules](https://github.com/byanto/niskora/issues/8), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 7. Levels and how advice is shown

- **RST-35** Every Single SKU with advice has one of three levels, from the days that available + on order lasts at the sales rate:

  | Level | Days it lasts |
  | --- | --- |
  | **Late** | fewer than the lead time, so it runs out even if ordered today |
  | **Order now** | fewer than the lead time plus safety days |
  | **Fine** | anything above |

- **RST-36** Advice is sorted by the days it lasts minus the lead time, the most negative first. It is grouped by supplier, then by [SPU](../../GLOSSARY.md#catalogue), so that one supplier's list is ready to become one purchase order.
- **RST-37** Advice can be filtered by buying channel, which is the buying channel of the preferred supply source.
- **RST-38** The view of one supplier lists every Single SKU of that supplier with a suggested quantity above zero, those still at Fine included. These top-ups are marked apart from Late and Order now, and each can be removed. One purchase order then brings the supplier's whole range up to target, and the next can wait a full order cycle.
- **RST-39** The navigation shows a count of the Single SKUs at Late or Order now.
- **RST-40** Every line shows its working: demand in the last 30 and the last 90 countable days, the sales rate, available, on order, the days it lasts, the lead time, the target days (lead time + order cycle + safety days), and the suggested quantity before and after rounding.
- **RST-41** While orders, the inventory list or stock movements are [stale](../../GLOSSARY.md#imports), the restock screen says so (FND-84). While any package is a [held package](../../GLOSSARY.md#imports), it warns as well (FND-86). Nothing is blocked.

_Sources: [Restock calculation rules](https://github.com/byanto/niskora/issues/8), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28), [Layout of the supplier purchase order PDF and the forwarder invoice](https://github.com/byanto/niskora/issues/20)._

## 8. From advice to a purchase order

- **RST-42** The advice of one supplier becomes a Draft purchase order: the Single SKUs left in the supplier's view, each with its suggested quantity after rounding.
- **RST-43** Each purchase order line made from advice keeps a copy of the working behind it: sales rate, available, on order, lead time and suggested quantity. The copy is frozen and never recalculated. It is the one figure in niskora that does not mean "as known now".
- **RST-44** Because a Draft counts for nothing, the advice stays as it is until the purchase order is Placed. From then on its lines are on order and the advice changes at once.

_Sources: [Restock calculation rules](https://github.com/byanto/niskora/issues/8), [Purchase order model and documents](https://github.com/byanto/niskora/issues/9), [Reporting and read model approach](https://github.com/byanto/niskora/issues/15)._

## 9. What is stored and when it is recalculated

- **RST-45** `restock` stores only the slow part, per Single SKU: the sales rate, the number of countable days and the 30-day rate.
- **RST-46** The stored part is recalculated when an import completes and when a campaign day is added, changed or removed.
- **RST-47** It is always recalculated in full, for every Single SKU, and never patched one by one. It carries a calculation version number. A release that changes the rule sees the mismatch at startup and recalculates by itself.
- **RST-48** Available, on order, the level and the suggested quantity are worked out live on each request. A Placed purchase order, a changed lead time or a changed setting shows at once, with nothing to refresh.
- **RST-49** `orders` hands over demand per Single SKU per business day and `stock` hands over stockout days. `restock` combines them in code. It reads no other module's tables ([ADR 0003](../adr/0003-one-database-schema-per-module.md)).
- **RST-50** Every figure reflects the data and the rules as they are now. A later cancellation, a filled stock history gap or a changed campaign day changes the sales rate, with no trace of the earlier figure.
- **RST-51** The restock screens and the agent's restock tools answer in under 1 second on ten years of synthetic data, as [engineering.md](engineering.md) requires (ENG-27).

_Sources: [Reporting and read model approach](https://github.com/byanto/niskora/issues/15), [Restock calculation rules](https://github.com/byanto/niskora/issues/8)._
