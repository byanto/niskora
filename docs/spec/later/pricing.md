# Pricing: the model

> **Not part of v1.** v1 builds nothing described here: no tables, no screens and no fields. Pricing is built as its own module, `pricing`, as the first feature after v1. This file records the model so that v1 leaves room for it.

How costs, selling prices, fees and margins are represented. Rule ids start with `PRC`.

Read [README.md](../README.md) first. Terms are defined in [`GLOSSARY.md`](../../../GLOSSARY.md) and are not redefined here.

## Check this

Points where the closed tickets disagree, or leave a gap that a rule below had to fill. The later ticket wins in each case. The owner confirms or corrects each one in the final review.

1. **Seeding from the Google Sheets workbooks (PRC-33, PRC-34).** The scope corrections said "the Google Sheet seeds it once". The later ticket on outside data found that a workbook row is a price group covering several Single SKUs, that matching rows to Merchant SKUs by name is unreliable, and that two of the three workbooks have not been examined. PRC-34 follows the later ticket: the load is part of building pricing and will most likely need a person to confirm each row. It is not an automatic one-off seed.
2. **Any excess raises the cost flag (PRC-28).** The scope corrections say niskora "warns when the actual cost rises above" the pricing cost. The pricing model says "any excess". PRC-28 has no tolerance, so a difference of one rupiah raises the flag.
3. **Which marketplaces are linked.** The pricing model names TikTok and Lazada as linked, Shopee as the anchor, and Manual and Blibli as independent. PRC-6 treats the role as a setting per marketplace, so a marketplace that appears later is given a role when it is first priced.
4. **A store's own price on a linked marketplace (PRC-11).** A store "can have its own typed price", which replaces the selling price, the minimum buy and the wholesale tiers as one set. The model does not say whether this is allowed on a linked marketplace, where prices are otherwise never typed. PRC-11 allows it on every marketplace, because the listing check records a deliberate difference on one store this way.
5. **A wholesale tier's planned profit (PRC-26).** "A wholesale tier spreads the fixed fees over the tier's own quantity" is read as: in the per-piece sum for a tier, fixed fees are divided by the tier's quantity and not by the minimum buy.

## 1. Scope

- **PRC-1** Pricing models normal prices only. Promotions, special prices, vouchers and flash sales are not modelled.
- **PRC-2** niskora will be the single home for prices. Prices and price ratios are still applied by a person, in BigSeller or in the marketplace. niskora pushes nothing.
- **PRC-3** Landed cost, exchange rates, and vouchers and delivery cost on a purchase are not part of this model.

_Sources: [Pricing model](https://github.com/byanto/niskora/issues/13), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 2. Cost

- **PRC-4** There are two costs. The [Weighted Average Cost](../../../GLOSSARY.md#cost) is BigSeller's figure, freight included, and is the only cost in v1. The [pricing cost](../../../GLOSSARY.md#cost) is the owner's deliberately padded figure, so that selling prices need not follow every change in the yuan rate or the forwarder's cost.
- **PRC-5** The pricing cost is set per [Single SKU](../../../GLOSSARY.md#catalogue) and can be set for a whole [SPU](../../../GLOSSARY.md#catalogue) at once. A [Combination SKU](../../../GLOSSARY.md#catalogue)'s pricing cost is the sum of its components'. niskora is the home of the pricing cost. BigSeller's Reference Cost Price is never imported.

_Sources: [Pricing model](https://github.com/byanto/niskora/issues/13), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17)._

## 3. Selling prices

- **PRC-6** Each [marketplace](../../../GLOSSARY.md#orders) has one role:

  | Role | Marketplaces today | The selling price |
  | --- | --- | --- |
  | [Anchor](../../../GLOSSARY.md#pricing) | Shopee, one setting for the whole business | is typed |
  | [Linked](../../../GLOSSARY.md#pricing) | TikTok, Lazada | is the anchor's selling price times a [price ratio](../../../GLOSSARY.md#pricing) |
  | [Independent](../../../GLOSSARY.md#pricing) | Manual, Blibli | is typed |

- **PRC-7** A [selling price](../../../GLOSSARY.md#pricing) is per [Merchant SKU](../../../GLOSSARY.md#catalogue) per marketplace. A Merchant SKU with no selling price on a marketplace is not sold there.
- **PRC-8** A linked selling price is rounded to the whole rupiah, as BigSeller's Information Linkage does. It is never typed, except for a Merchant SKU that has no anchor price.
- **PRC-9** The price ratio is per SPU per linked marketplace. A Merchant SKU priced differently from the rest of its SPU can have its own, and so can a Combination SKU. niskora records the ratio a user has set in BigSeller.
- **PRC-10** The [minimum buy](../../../GLOSSARY.md#pricing) is per Merchant SKU per marketplace and is 1 unless set.
- **PRC-11** A store can have its own typed price for a Merchant SKU. It replaces the selling price, the minimum buy and the wholesale tiers together, as one set.
- **PRC-12** [Wholesale tiers](../../../GLOSSARY.md#pricing) are pairs of quantity and price. A selling price can have several, only on marketplaces marked as allowing them, which today are Shopee and Manual. Each tier's quantity is above the minimum buy and above the tier before it, and its price is below the tier before it.
- **PRC-13** A Combination SKU has its own selling price. A cheaper bundle is simply a lower typed price, and niskora shows it against the sum of its components' prices. There is no discount concept.

_Sources: [Pricing model](https://github.com/byanto/niskora/issues/13)._

## 4. Fees and allowances

- **PRC-14** A [fee schedule](../../../GLOSSARY.md#pricing) belongs to a marketplace and applies from a stated date, so that a coming change can be entered ahead.
- **PRC-15** A [fee](../../../GLOSSARY.md#pricing) is either a percentage of the price, with an optional cap in rupiah and possibly varying by [marketplace category](../../../GLOSSARY.md#pricing), or a fixed amount per order. Manual has an empty schedule.
- **PRC-16** All stores on a marketplace share its schedule. The model leaves room for a store to be given a whole schedule of its own later.
- **PRC-17** Each SPU and each Combination SKU has one marketplace category per marketplace, and a special-size mark.
- **PRC-18** An [allowance](../../../GLOSSARY.md#pricing) is the owner's own percentage per marketplace, for ads, affiliates or miscellaneous cost. It is kept apart from fees.

_Sources: [Pricing model](https://github.com/byanto/niskora/issues/13)._

## 5. Margin, advice and profit

- **PRC-19** The [target margin](../../../GLOSSARY.md#pricing) has one default per [company](../../../GLOSSARY.md#catalogue), with an override per SPU or Combination SKU. It is the same on every marketplace.
- **PRC-20** The [recommended price](../../../GLOSSARY.md#pricing) is given on the anchor and the independent marketplaces only:

  recommended price = (pricing cost + fixed fees / minimum buy) / (1 - fee percentages - allowance percentages - target margin)

- **PRC-21** The recommended price is not rounded to a price ending. It is advice, and a person types the selling price.
- **PRC-22** The [recommended ratio](../../../GLOSSARY.md#pricing) is given on linked marketplaces only: the ratio that gives the same planned margin as the anchor's actual selling price, rounded up to two decimals.
- **PRC-23** Where the Merchant SKUs of an SPU need different ratios, the SPU's recommended ratio is the largest among those that have no ratio of their own.
- **PRC-24** Profit per piece = price - price x (fee and allowance percentages) - fixed fees / minimum buy - cost. Margin is profit / price.
- **PRC-25** [Planned profit](../../../GLOSSARY.md#pricing) uses the pricing cost. [Real profit](../../../GLOSSARY.md#pricing) uses the Weighted Average Cost.
- **PRC-26** For a wholesale tier, the fixed fees are spread over the tier's own quantity.

_Sources: [Pricing model](https://github.com/byanto/niskora/issues/13)._

## 6. Flags

- **PRC-27** niskora shows the gap between the two costs.
- **PRC-28** niskora raises a flag when:

  1. the Weighted Average Cost is above the pricing cost, by any amount;
  2. the planned margin is below the target margin;
  3. the real profit is below zero;
  4. the recorded price ratio is below the recommended ratio.

_Sources: [Pricing model](https://github.com/byanto/niskora/issues/13), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17)._

## 7. History and checking

- **PRC-29** Every change to a pricing cost, a selling price, a wholesale tier, a price ratio, a fee schedule or an allowance is kept with its date and its user.
- **PRC-30** A later slice of pricing imports BigSeller's listing exports and compares each listing's normal `Price` with niskora's selling price. The two must be equal, with one rupiah of slack on linked marketplaces. `Special Price` is ignored.
- **PRC-31** A deliberate difference on one store is recorded as that store's own price (PRC-11). niskora does not otherwise manage [listings](../../../GLOSSARY.md#catalogue).
- **PRC-32** Prices in order exports are never checked against selling prices, because they include promotions.

_Sources: [Pricing model](https://github.com/byanto/niskora/issues/13)._

## 8. Where the first figures come from

- **PRC-33** v1 brings in nothing from the Google Sheets workbooks or from Grist. The workbooks stay in use until pricing is built.
- **PRC-34** Loading the workbooks is part of building pricing. A workbook row is a price group that covers several Single SKUs, while the pricing cost is set per Single SKU, so a person confirms the match for each row.
- **PRC-35** Still to examine when pricing is built: the workbook codes that have no SPU in BigSeller, and the two workbooks that have not been looked at.
- **PRC-36** Deriving linked prices through BigSeller's ratio is the present arrangement and not a lasting commitment. With marketplace API access niskora could push prices itself. That would be a new decision.

_Sources: [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28), [Pricing model](https://github.com/byanto/niskora/issues/13), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17)._
