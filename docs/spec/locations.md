# Locations: customer location analysis

Where packages go, by province, city and district, and for which products. Rule ids start with `LOC`.

Read [README.md](README.md) first, then [foundation.md](foundation.md), which defines [packages](../../GLOSSARY.md#orders), [destinations](../../GLOSSARY.md#orders) and [demand](../../GLOSSARY.md#orders). Terms are defined in [`GLOSSARY.md`](../../GLOSSARY.md) and are not redefined here.

## Check this

Points where the closed tickets disagree, or leave a gap that a rule below had to fill. The later ticket wins in each case. The owner confirms or corrects each one in the final review.

1. **Where the official list of places comes from (LOC-20).** The decision says niskora ships "the current government list (Kemendagri codes, 38 provinces)". The boundary file research names one machine-readable copy (`cahyadsn/wilayah`, MIT licence) as a side note and did not assess it. LOC-20 fixes only what the list must be. The file itself is chosen and checked against the government decree in the build ticket.
2. **What a spelling is (LOC-25, LOC-27).** The tickets map "a spelling" to a place, and match a city inside its province and a district inside its city. They do not say what a place alias is keyed on. LOC-25 reads it as: the level, the cleaned text and the place above it. An alias is shared by all marketplaces, although the correction page shows which marketplace each spelling came from.
3. **The company filter (LOC-13).** A store belongs to at most one company and may sell any company's Merchant SKUs, so "company" could mean the store's or the Merchant SKU's. LOC-13 reads it as the company of the Merchant SKU on the line, the same way demand is narrowed to a company.
4. **Value under a company filter (LOC-10, LOC-13).** A view filtered by SPU or Merchant SKU leaves the value of Combination SKUs out. The decision does not name the company filter. LOC-13 treats it like the unfiltered view: the Combination SKU's own line counts with its value under the company of that Combination SKU, and its component lines add no value.
5. **Unmapped lines (LOC-5).** The history ticket says an unmapped line's package "still counts in location totals (packages, units, goods value)". LOC-5 counts it in totals and in views filtered by marketplace or store only. It can never appear under an SPU, a Merchant SKU or a company.
6. **Unmatched destinations in the import report (LOC-30).** The report is owned by `imports`, which `locations` depends on. How the spellings reach the report without a dependency in the other direction is not settled. It is point 3 of "Check this" in [engineering.md](engineering.md).
7. **Large islands missing from the map (LOC-37).** The simplified boundary file loses 12 polygons of 100 km² or more. The research did not find out why. It is looked at when the file is built.
8. **The boundary file's licence (LOC-36).** BIG states no licence. Committing the file rests on a reading of the law, not on legal advice. If niskora is ever sold, BIG must be asked first.

## 1. Purpose and what is counted

- **LOC-1** The analysis exists to decide where to aim ads that target by place, and for which products.
- **LOC-2** It counts packages by destination. v1 keeps no customer record and does no repeat-buyer analysis.
- **LOC-3** The packages counted are the ones that count as demand: every package that is not Canceled, not an [abandoned package](../../GLOSSARY.md#orders) and not a [hub package](../../GLOSSARY.md#orders), on the business day of its Order Time. A returned package counts. A [held package](../../GLOSSARY.md#imports) is not in the order history and is not counted.
- **LOC-4** There are three measures: packages, units and goods value. Goods value is the selling prices of the package lines, before fees, vouchers and shipping.
- **LOC-5** An [unmapped line](../../GLOSSARY.md#orders) counts in a place's totals with its units and its value, and under no [SPU](../../GLOSSARY.md#catalogue), Merchant SKU or company.
- **LOC-6** Each measure is shown with its share of the total, with the figure for the preceding period of the same length, and with the change between the two.
- **LOC-7** Every figure reflects the data and the rules as they are now. A later cancellation or a new place alias changes past figures.

_Sources: [Customer location analysis](https://github.com/byanto/niskora/issues/10), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17), [Import rule changes from the full history](https://github.com/byanto/niskora/issues/23), [Hub package and final status rules after the second order export](https://github.com/byanto/niskora/issues/24), [Reporting and read model approach](https://github.com/byanto/niskora/issues/15)._

## 2. Combination SKUs

- **LOC-8** An unfiltered total counts a [Combination SKU](../../GLOSSARY.md#catalogue)'s own line once, with its value. Its [component lines](../../GLOSSARY.md#orders) add no value.
- **LOC-9** Units are counted through the component lines, as in demand.
- **LOC-10** A view filtered by SPU or Merchant SKU counts the units sold inside Combination SKUs and leaves their value out. The screen says so: "includes N units sold inside combinations, value not included".
- **LOC-11** A Combination SKU's price is never split across its components.

_Sources: [Customer location analysis](https://github.com/byanto/niskora/issues/10)._

## 3. Views and filters

- **LOC-12** Province, city and district are equal levels, with drill-down from one to the next. The village level that Blibli provides is not used. Post code is not a level.
- **LOC-13** The filters are: a date range (the last 90 days by default), [marketplace](../../GLOSSARY.md#orders), [store](../../GLOSSARY.md#orders), [company](../../GLOSSARY.md#catalogue), and SPU or Merchant SKU. Company means the company of the Merchant SKU on the line.
- **LOC-14** There are two directions: "where does this SPU sell", which ranks places for a product, and "what sells in this place", which ranks SPUs for a place.
- **LOC-15** Each level states which marketplaces it covers. Today Shopee and Blibli reach the district, TikTok reaches the city, and Lazada masks every level.
- **LOC-16** Packages with an unknown destination are shown as their own row with its share. They are left out of the percentages by default and are never spread across known places.
- **LOC-17** A package matched at an upper level only counts there and is unknown below it.
- **LOC-18** The results are ranked tables with bars, and one map of the provinces (section 6). There is no city map and no district map.
- **LOC-19** Each table can be downloaded as CSV, holding exactly what the table shows.

_Sources: [Customer location analysis](https://github.com/byanto/niskora/issues/10), [v1 scope corrections from the export findings](https://github.com/byanto/niskora/issues/17)._

## 4. Places and automatic matching

- **LOC-20** niskora ships the official list of [places](../../GLOSSARY.md#locations): the government's current provinces, cities and districts with their Kemendagri codes, 38 provinces among them. The list is updated with releases and is not edited by users.
- **LOC-21** The lowest matched level decides the levels above it. A kabupaten lands in its current province whatever the marketplace wrote.
- **LOC-22** Automatic matching is exact only, after ignoring letter case, punctuation and the words Kota, Kab., Kabupaten and Kota Administrasi. There is no fuzzy guessing.
- **LOC-23** A seeded list of known variants is matched as well, such as English province names, "NAD" and "D.I. Aceh".
- **LOC-24** A city is matched inside its province, and a district inside its city.
- **LOC-25** A [place alias](../../GLOSSARY.md#locations) maps one spelling to one place. It is keyed on the level, the cleaned text and the place above it, and applies to every marketplace.
- **LOC-26** A destination whose spelling maps to no place is an [unmatched destination](../../GLOSSARY.md#locations). Its package counts as unknown below the last level that did match.

_Sources: [Customer location analysis](https://github.com/byanto/niskora/issues/10), [Boundary file for the province map](https://github.com/byanto/niskora/issues/31)._

## 5. Corrections

- **LOC-27** A correction page lists every unmatched spelling with its marketplace and its number of packages, the biggest first.
- **LOC-28** A user maps a spelling to a place. The mapping applies at once to every past and future package with that spelling. It can be changed at any time, and niskora records who changed it and when.
- **LOC-29** niskora maps spellings and never sets a destination on a single package. A package with a masked or empty destination stays "Location unknown". A manual order is fixed in BigSeller, and the next export brings it in.
- **LOC-30** Unmatched spellings also appear in the import report of the order import that brought them (FND-79).
- **LOC-31** `orders` keeps the destination text exactly as exported (FND-57). `locations` owns the place list, the place aliases and the correction page. A mapping change therefore never touches the order history, and a re-import never loses a correction.
- **LOC-32** `orders` hands over packages, units and goods value per destination text, already counted. `locations` rolls them up into places in code and reads no other module's tables ([ADR 0003](../adr/0003-one-database-schema-per-module.md)).

_Sources: [Customer location analysis](https://github.com/byanto/niskora/issues/10), [Reporting and read model approach](https://github.com/byanto/niskora/issues/15)._

## 6. The province map

- **LOC-33** The map shows the 38 provinces, coloured in ranked steps, so that the provinces behind the dominant one still differ.
- **LOC-34** It is drawn from one TopoJSON file committed to the repository. Nothing is fetched from a map service at runtime.
- **LOC-35** The file comes from Badan Informasi Geospasial (BIG): the 1:50,000 regency and city boundaries, June 2026 edition, dissolved into provinces and simplified at 2 km. It is about 75 KB.
- **LOC-36** The map carries the credit "Badan Informasi Geospasial, 1:50.000, edisi Juni 2026, disederhanakan".
- **LOC-37** The file's object is `provinces`, and each shape has a property `code` holding the two-digit Kemendagri province code. The map joins on that code and takes every display name from niskora's own place list, never from the file.
- **LOC-38** The file is produced once, by hand, with these steps, and the result is committed:

  1. For each province code `NN`, download `https://geoservices.big.go.id/rbi/rest/services/BATASWILAYAH/BATAS_KABKOTA_AR/MapServer/0/query?where=KDPPUM='NN'&outFields=KDPPUM&outSR=4326&maxAllowableOffset=0.0005&f=geojson`. The service is slow and fails at times, so a download is tried again.
  2. Run `npx mapshaper@0.6.102 -i k*.geojson combine-files -merge-layers force name=provinces -dissolve2 KDPPUM -rename-fields code=KDPPUM -clean -simplify interval=2000 keep-shapes -clean -o provinces.topojson format=topojson quantization=10000`. The first `-clean` removes the slivers between regencies and must not be dropped.

_Sources: [Boundary file for the province map](https://github.com/byanto/niskora/issues/31), [Customer location analysis](https://github.com/byanto/niskora/issues/10)._

## 7. Personal data and speed

- **LOC-39** No [buyer](../../GLOSSARY.md#orders) field ever appears in the analysis or in a download: no name, phone, address, post code, Shopee username or Customer Code. They show only on a single package's own page (FND-56).
- **LOC-40** `locations` may not depend on the buyer part of `orders`. The build fails if it does (ENG-21).
- **LOC-41** While orders are [stale](../../GLOSSARY.md#imports), the analysis says so (FND-84). Nothing is blocked.
- **LOC-42** The analysis screens and the agent's location tool answer in under 1 second on ten years of synthetic data (ENG-27).

_Sources: [Customer location analysis](https://github.com/byanto/niskora/issues/10), [Reporting and read model approach](https://github.com/byanto/niskora/issues/15)._
