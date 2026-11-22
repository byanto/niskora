# Frontend: architecture, screens and visual design

How niskora appears to a person: the shell and its navigation, the screens, the libraries the React app stands on, the conventions every screen follows, and the look. Rule ids start with `FE`.

Read [README.md](README.md) first. Terms are defined in [`GLOSSARY.md`](../../GLOSSARY.md) and are not redefined here.

This file says which screens exist and what each one shows. What the figures on a screen mean, and what an action does, is in the area file the screen belongs to: [foundation.md](foundation.md), [restock.md](restock.md), [purchasing.md](purchasing.md), [locations.md](locations.md), [agent.md](agent.md) and [platform.md](platform.md). Where this file and an area file disagree about behaviour, the area file wins.

## 1. Shell and navigation

- **FE-1** Every screen except Login sits inside one shell: a fixed left sidebar, and above the screen a search box and a data freshness chip.
- **FE-2** The sidebar groups the screens by job:

  | Group | Screens |
  | --- | --- |
  | Daily | Home, Imports |
  | Catalogue | SPUs and Merchant SKUs, Packages |
  | Buying | Restock advice, Purchase orders, Match stock-ins, Suppliers |
  | Analysis | Location analysis, Ask niskora |
  | Setup | Campaign days, Place aliases, Forwarders, Users, Settings |

- **FE-3** The search box takes a code, a number or a name and opens the matching [SPU](../../GLOSSARY.md#catalogue), [Merchant SKU](../../GLOSSARY.md#catalogue), [package](../../GLOSSARY.md#orders) (by Package No or Order No), [purchase order](../../GLOSSARY.md#purchasing) or [supplier](../../GLOSSARY.md#purchasing). It never searches buyer fields.
- **FE-4** The data freshness chip says how many data sets are [stale](../../GLOSSARY.md#imports), or that all are fresh. It refreshes every minute and links to Imports. When a data set is stale is decided in [foundation.md](foundation.md).
- **FE-5** The sidebar carries three counts: needs attention on Home, Late or Order now on Restock advice (without the "No supplier" block, RST-39), and [unmatched stock-ins](../../GLOSSARY.md#purchasing) on Match stock-ins.
- **FE-6** The sidebar shows that an import is running, on every screen, until its report is ready.
- **FE-7** A detail page opens from its list and shows a way back to that list. The detail pages are Import report, SPU page, Merchant SKU page, Package page, Purchase order page and Supplier page.
- **FE-8** The sidebar shows the name of the logged-in [user](../../GLOSSARY.md#people) with "Log out" and a way to change their own password.

Not chosen: a top bar grouped by capability, and a worklist home with an icon rail and the agent beside every screen.

_Sources: [Main screens and navigation](https://github.com/byanto/niskora/issues/25), [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28)._

## 2. Screens

v1 has 22 screens. Each has its own URL.

| No. | Screen | Group | Feature folder |
| --- | --- | --- | --- |
| 1 | Home | Daily | `app` |
| 2 | Imports | Daily | `imports` |
| 3 | Import report | Daily | `imports` |
| 4 | Catalogue (SPUs, Merchant SKUs, Combination SKUs) | Catalogue | `catalogue` |
| 5 | SPU page | Catalogue | `catalogue` |
| 6 | Merchant SKU page | Catalogue | `catalogue` |
| 7 | Packages | Catalogue | `orders` |
| 8 | Package page | Catalogue | `orders` |
| 9 | Restock advice | Buying | `restock` |
| 10 | Purchase orders | Buying | `purchasing` |
| 11 | Purchase order page | Buying | `purchasing` |
| 12 | Match stock-ins | Buying | `purchasing` |
| 13 | Suppliers | Buying | `purchasing` |
| 14 | Supplier page | Buying | `purchasing` |
| 15 | Location analysis | Analysis | `locations` |
| 16 | Ask niskora | Analysis | `agent` |
| 17 | Campaign days | Setup | `restock` |
| 18 | Place aliases | Setup | `locations` |
| 19 | Forwarders | Setup | `purchasing` |
| 20 | Users | Setup | `identity` |
| 21 | Settings | Setup | `purchasing`, `catalogue` |
| 22 | Login | none | `identity` |

### Daily

- **FE-9** **Home** is an overview, not a numbered worklist. It shows:
  1. Data freshness per data set, each with the time of its last [import](../../GLOSSARY.md#imports) and a Fresh or Stale badge.
  2. The "needs attention" list. Every item has a short explanation and a link to the screen where it is dealt with.
  3. Restock advice counts: Late, Order now, and the number of suppliers with something to order.
  4. Purchase order counts: placed and waiting (with how many are overdue), drafts, and arriving this week.
  5. Yesterday: packages and units of [demand](../../GLOSSARY.md#orders).
- **FE-10** The items of "needs attention" are defined by the area files, not here. Each item has a severity, shown with the icons of FE-105. The ticket on outside data adds two: Single SKUs with no supply source, and SPUs whose prefix gives no [company](../../GLOSSARY.md#catalogue).
- **FE-11** Business conditions, such as stale data, [held packages](../../GLOSSARY.md#imports), a rejected file and the agent's limit being reached, show on Home and nowhere outside the app ([platform.md](platform.md)).
- **FE-12** **Imports** has one drop zone for every [export](../../GLOSSARY.md#imports) type, taking several files at once. Beside it, the screen names the order export layout to pick, "by Merchant SKU" (FND-95). Below it are the daily checklist (each data set, how often it is due and how fresh it is) and the list of imports: when, file, detected data set, the range it covers, accepted or rejected, what changed, and who uploaded it.
- **FE-13** **Import report** shows one import: the file, the data set, who uploaded it and when, accepted or rejected, the export time read from the file name, the counts of new, changed, unchanged, held and [not seen](../../GLOSSARY.md#imports), the other findings, and a link to download the original file. The report of an order import links to the Place aliases screen for unmatched spellings and does not list them. A rejected import shows the reason.

### Catalogue

- **FE-14** **Catalogue** has three tabs: the SPU list, the flat Merchant SKU list and the [Combination SKU](../../GLOSSARY.md#catalogue) list. Each is a paged table with search and filters (company, active or not, restock level, supplier) and a row photo.
- **FE-15** **SPU page** shows the SPU's photo and totals, then one row per Merchant SKU with [on hand](../../GLOSSARY.md#stock), [allocated](../../GLOSSARY.md#stock), [available](../../GLOSSARY.md#stock), [on order](../../GLOSSARY.md#purchasing), [sales rate](../../GLOSSARY.md#restock), days left, [Weighted Average Cost](../../GLOSSARY.md#cost) and restock level. It names the Combination SKUs that contain its Merchant SKUs and links to the location analysis for the SPU.
- **FE-16** On the SPU page a user can pick which Merchant SKU's photo stands for the SPU. Until one is picked it is the photo of the first Merchant SKU. There is no photo upload in v1.
- **FE-17** **Merchant SKU page** shows the stock figures, the demand per [business day](../../GLOSSARY.md#imports) as a chart with [stockout days](../../GLOSSARY.md#restock) and [campaign days](../../GLOSSARY.md#restock) marked, the restock advice with its working, the supply sources (with adding and editing), and the [stock movements](../../GLOSSARY.md#stock).
- **FE-18** A Merchant SKU with no photo yet, or whose photo failed to download, shows a neutral placeholder of the same size on every screen.
- **FE-19** **Packages** is a paged table with tabs for all, held, [abandoned](../../GLOSSARY.md#orders), [hub](../../GLOSSARY.md#orders) and with [unmapped lines](../../GLOSSARY.md#orders), and filters for marketplace, store, status and period.
- **FE-20** **Package page** shows the [package lines](../../GLOSSARY.md#orders) with their [component lines](../../GLOSSARY.md#orders), whether the package counts as demand, the [destination](../../GLOSSARY.md#orders) as exported and as matched, the shipping fields and the [buyer](../../GLOSSARY.md#orders) fields. The buyer fields appear on this page only: never in a list, a CSV, a search result or an agent answer.

### Buying

- **FE-21** **Restock advice** shows one collapsible block per supplier. Inside a block are SPU rows with their photo, and under each SPU one line per Single SKU with available, on order, sales rate, days left, the restock level, an editable quantity and the working. Top-ups can be switched on and off. Filters: search, company, restock level and buying channel.
- **FE-22** A "No supplier" block holds the Single SKUs that have advice and no supply source. A user ticks SPUs in it and assigns a supplier. No purchase order can be made from this block.
- **FE-23** Every other supplier block has a footer with the selected lines, units, goods total and, when [carton](../../GLOSSARY.md#purchasing) data exists, the volume estimate, and a "Create draft purchase order" button.
- **FE-24** When a data set the advice depends on is stale, the Restock advice screen says so above the blocks, with a link to Imports.
- **FE-25** **Purchase orders** is a paged table with tabs by status and a tab for [shortfalls](../../GLOSSARY.md#purchasing), and filters for supplier and search.
- **FE-26** **Purchase order page** groups the lines by SPU and shows per line the ordered and [received quantities](../../GLOSSARY.md#purchasing), the [goods price](../../GLOSSARY.md#purchasing), the line state and the shortfall outcome. It also shows the totals, the volume estimate (only when carton data exists), the documents, the matched stock-ins and the audit trail.
- **FE-27** The Purchase order page shows the purchase order's buying channel, which a user can change while the purchase order is a Draft. A line that has no supply source for the new buying channel is flagged. The page offers only the documents that the buying channel allows ([purchasing.md](purchasing.md)).
- **FE-28** **Match stock-ins** shows the unmatched [Purchase Stock-Ins](../../GLOSSARY.md#stock) on the left. On the right is the best matching purchase order for the chosen one, with what confirming would do to each line, and the actions to confirm, to match another purchase order, or to mark the stock-in as bought outside niskora.
- **FE-29** **Suppliers** is a paged table of suppliers with "New supplier". The [supply source sheet](../../GLOSSARY.md#purchasing) is downloaded and uploaded here, and never on the Imports screen. A rejected sheet lists every faulty row with its reason.
- **FE-30** **Supplier page** shows the supplier's ordering figures, contact and optional link, its purchase orders with the units it still owes, its supply sources (set for a whole SPU or per Merchant SKU, editable on screen, removable on screen only) and the cartons per SPU.
- **FE-31** The supplier's link and a supply source's product link show on screen only and never on a document.
- **FE-32** **Forwarders** lists each [forwarder](../../GLOSSARY.md#purchasing) with its [marking code](../../GLOSSARY.md#purchasing) and its [forwarder warehouses](../../GLOSSARY.md#purchasing), with adding and editing.

### Analysis

- **FE-33** **Location analysis** works in both directions ([places](../../GLOSSARY.md#locations) for an SPU, and SPUs for a place), with province, city and district as tabs, a period compared with the one before it, a choice of measure (packages, units, goods value) and a marketplace filter. It shows a ranked table that includes the unknown and the unmatched rows, the province map (section 10) and a CSV download.
- **FE-34** **Ask niskora** is the only place the [agent](../../GLOSSARY.md#agent) appears. The user's own [conversations](../../GLOSSARY.md#agent) are listed beside the open one. No other screen has an agent panel.

### Setup

- **FE-35** **Campaign days** lists the marked days with who marked each, lists the days that look like a campaign and are not marked, and has a form to mark a day or a range. **Place aliases** has tabs for unmatched spellings, all [place aliases](../../GLOSSARY.md#locations) and the official places, and lets a user map a spelling to a place.
- **FE-36** **Settings** shows the company prefix table, which is read-only, and four editable settings: the default lead time and the default order cycle used for a Single SKU with no supply source (RST-22), the safety days (RST-18) and the trading name printed on the Daftar Pesanan (PUR-70). Every change is recorded with who made it and when.
- **FE-37** **Users** lists every account, with "Add user", "Reset password", and "Disable" or "Enable" ([platform.md](platform.md), PLT-45). A user sees no "Reset password" or "Disable" on their own row: they change their own password from the sidebar (FE-8), which asks for the current one. There are no roles in v1.
- **FE-38** **Login** takes a login name and a password and stands outside the shell.

_Sources: [Main screens and navigation](https://github.com/byanto/niskora/issues/25), [Bringing in the Google Sheets and Grist data](https://github.com/byanto/niskora/issues/28), [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), the `prototype/main-screens` branch._

## 3. Architecture

- **FE-39** The frontend is a plain client-rendered single page app, built with Vite and pnpm in strict TypeScript and served as static files by Caddy. There is no server rendering and no meta-framework.
- **FE-40** Routing is TanStack Router with file-based routes.
- **FE-41** Every filter, tab, period, sort and page number is a typed search parameter in the URL. Every view is therefore linkable, which is what lets the agent give a link to a screen.
- **FE-42** Everything that comes from the backend is held by TanStack Query. There is no global client store. View state lives in the URL and the rest in components.
- **FE-43** The API types are generated with `openapi-typescript` from the committed OpenAPI document and called through `openapi-fetch` ([engineering.md](engineering.md)). Query definitions are written by hand, per module.
- **FE-44** Ids from the API are branded types in the frontend, so a supplier id cannot be passed where a purchase order id is expected.
- **FE-45** Components are shadcn/ui, copied into the repository, styled with Tailwind CSS. Icons come from Lucide only.
- **FE-46** Colours, spacing, corner radii and type sizes are named tokens in the Tailwind theme, with the rules written in `docs/design.md`. A component never uses a raw colour value, so the look can be changed in one place.
- **FE-47** Charts use Recharts. v1 has one chart: demand per business day on the Merchant SKU page.
- **FE-48** There is no Storybook.

_Sources: [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [API conventions](https://github.com/byanto/niskora/issues/30)._

## 4. Folder layout

- **FE-49** Inside `frontend/src`:

  | Folder | Holds |
  | --- | --- |
  | `app/` | the shell (sidebar, search box, data freshness chip), the providers and Home |
  | `features/<module>/` | one folder per backend module that has screens (`catalogue`, `orders`, `imports`, `purchasing`, `restock`, `locations`, `agent`, `identity`), with its screens, components, query definitions and tests side by side |
  | `routes/` | thin route files: each declares the URL and its search parameters and renders a feature screen |
  | `ui/` | the shadcn components |
  | `lib/` | the API client, formatting, translations and the search parameters shared by routes |
  | `test/` | the test setup: the fake network, rendering a route and the axe check |

- **FE-50** A lint rule forbids one feature from importing another feature's internals.

_Sources: [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26)._

## 5. Conventions on every screen

- **FE-51** **Loading:** a skeleton in the shape of the content. Never a blank page and never a full-page spinner.
- **FE-52** **Empty:** the screen says what is missing and offers the next action.
- **FE-53** **Failed load:** the translated Problem Details message with a retry button, inside the shell.
- **FE-54** **Saving:** the button shows that it is saving, and the screen changes only after the server answers. There are no optimistic updates.
- **FE-55** When an action fails, field errors show on their fields and any other error shows as an alert next to the action, just above its buttons. The alert stays until the user tries again. A toast is only for a short success message, never for an error: it shows away from the action, disappears, and a screen reader cannot reach it again.
- **FE-56** Every error code from the backend maps to a translation key, both the top-level code of a Problem Details response and the code of each field error. A field error is placed on its field by its JSON Pointer.
- **FE-57** Enum values from the API, such as `ORDER_NOW`, are mapped to translation keys and never shown raw.
- **FE-58** **Accessibility:** WCAG 2.1 AA. State is never shown by colour alone.

_Sources: [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [API conventions](https://github.com/byanto/niskora/issues/30)._

## 6. Tables and forms

- **FE-59** The server does all paging, sorting and filtering. A table shows 50 rows per page, with the page number in the URL.
- **FE-60** TanStack Table supplies the table logic. There is no virtual scrolling and no infinite scroll.
- **FE-61** Numbers in tables and figures use tabular figures and are right-aligned.
- **FE-62** Codes (Merchant SKU, SPU, Package No, purchase order numbers) and the restock working are set in the monospace typeface.
- **FE-63** Forms use React Hook Form, with Zod for shape checks only: required, number and range. The backend is the authority on every business rule, and its field errors are shown on the matching fields.

_Sources: [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [Visual design: palette, typeface and density](https://github.com/byanto/niskora/issues/32)._

## 7. Language, numbers and dates

- **FE-64** Text comes from i18next (`react-i18next`) with typed keys and one namespace per backend module. No user-facing text is written straight into a component.
- **FE-65** The UI is in English. Bahasa Indonesia can be added later as a second set of translations and is not part of v1.
- **FE-66** Numbers and dates are shown in the Indonesian form: `1.250.000`, `16 Nov 2026`, and 24-hour times in Jakarta time. Month names are English and times take a colon, to match the English UI: `16 May 2026`, `14:05`.
- **FE-67** The backend sends business dates as plain dates and moments in UTC. The frontend only formats them, with `Intl`.
- **FE-68** The frontend never works out "today", "yesterday" or a period by itself. The backend does.
- **FE-69** Terms on screen are the terms of [`GLOSSARY.md`](../../GLOSSARY.md), spelled the same way.
- **FE-70** Money arrives as an amount with its currency. It is formatted with `Intl` in the Indonesian locale: rupiah as `Rp 1.250.000`, and any other currency with its own symbol.

_Sources: [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [API conventions](https://github.com/byanto/niskora/issues/30)._

## 8. Session

- **FE-71** On start the app asks the backend who is logged in.
- **FE-72** A 401 from any call sends the user to Login, and after logging in back to the same URL.
- **FE-73** CSRF protection stays on. The API client echoes Spring Security's token cookie in a header on every changing request.
- **FE-74** The session lives in a server-side session cookie ([platform.md](platform.md)). Nothing else about the session is kept in the browser.

_Sources: [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [Deployment, login and roles](https://github.com/byanto/niskora/issues/12)._

## 9. Data between the app and the backend

- **FE-75** **Staying current:** there is no live push. A screen reloads its data on return to the browser tab, on navigation and after the user's own save.
- **FE-76** **Uploads:** the drop zone shows upload progress. The backend answers once the file is stored, with the import marked "processing". The Imports screen then checks every 2 seconds until the report is ready. The user can leave the page meanwhile (FE-6).
- **FE-77** When an uploaded file is identical to one already imported successfully, the backend points at that import, and the screen says so with a link to its report.
- **FE-78** **Agent answers** stream as Server-Sent Events read from a `fetch` POST. There is no WebSocket.
- **FE-79** **Downloads:** every file is made by the backend and fetched by a plain link: the location analysis CSV, the [Daftar Pesanan](../../GLOSSARY.md#purchasing), the [forwarder invoice](../../GLOSSARY.md#purchasing), the BigSeller import file, the supply source sheet and an import's original file. The frontend builds no file.
- **FE-80** **Repeated requests:** the API client makes one `Idempotency-Key` per user intent, when the user presses the button, and reuses it on a retry of that same press ([engineering.md](engineering.md)). The client retries a press by itself, with its key, when the request got no answer or the backend says the first try is still running (409). Any other failure is shown at once.
- **FE-81** **Two users editing the same thing:** the API client sends the version it read back as `If-Match` on every change. When the backend refuses the change with 412, the screen keeps what the user typed, says who changed the thing and when, and offers one button that loads the latest version. Nothing is merged or retried automatically.
- **FE-82** **A new version:** every response carries the backend's build id. When it differs from the build the frontend was made with, the shell shows "A new version is available, reload".
- **FE-83** **Local development:** the Vite dev server forwards `/api` to the backend in Compose, so the browser sees one origin, as in production. MSW fakes the API in tests only.

_Sources: [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [API conventions](https://github.com/byanto/niskora/issues/30)._

## 10. The province map

- **FE-84** The map on the Location analysis screen is plain SVG, drawn from a boundary file committed to the repository and projected with `d3-geo`. There is no tile map, and nothing is fetched from a map service at runtime.
- **FE-85** The boundary file is one TopoJSON file of Indonesia's 38 provinces, about 75 KB (23 KB gzipped), with the object `provinces` and the property `code` on each province.
- **FE-86** `code` is the Kemendagri province code (`"11"` to `"96"`). The map joins its figures on that code and never on a name. Province names on screen come from niskora's own list of places ([locations.md](locations.md)).
- **FE-87** The file is made once, by hand, from Badan Informasi Geospasial's 1:50,000 regency and city boundaries (June 2026 edition), dissolved into provinces and simplified at 2 km:
  1. Per province code `NN`, fetch `https://geoservices.big.go.id/rbi/rest/services/BATASWILAYAH/BATAS_KABKOTA_AR/MapServer/0/query?where=KDPPUM='NN'&outFields=KDPPUM&outSR=4326&maxAllowableOffset=0.0005&f=geojson`. The service is slow and at times answers HTTP 500, so retry.
  2. Run `npx mapshaper@0.6.102 -i k*.geojson combine-files -merge-layers force name=provinces -dissolve2 KDPPUM -rename-fields code=KDPPUM -clean -simplify interval=2000 keep-shapes -clean -o provinces.topojson format=topojson quantization=10000`. The first `-clean` removes the slivers between regencies and must stay.
- **FE-88** Islands under 10 km² are not drawn. A few larger islands also drop out in the simplifying, and they are looked at when the file is built.
- **FE-89** The credit "Badan Informasi Geospasial, 1:50.000, edisi Juni 2026, disederhanakan" shows under the map and sits beside the file in the repository.
- **FE-90** The file may be committed to a public repository. If niskora is ever sold, Badan Informasi Geospasial's permission is asked first, because a derivative made for sale needs it.

_Sources: [Boundary file for the province map](https://github.com/byanto/niskora/issues/31), [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [Customer location analysis](https://github.com/byanto/niskora/issues/10)._

## 11. Visual design

The look is variant D of the visual design prototype: cool slate neutrals with a deep teal accent. It is roomier than a compact density and tighter than the other directions shown.

### Typeface and type sizes

- **FE-91** All text is set in Geist, and codes in Geist Mono (FE-62). Both are self-hosted in the app build. Nothing is loaded from a font service at runtime.
- **FE-92** Type sizes:

  | Token | Size | Use |
  | --- | --- | --- |
  | `text-xs` | 12 px | labels, table headers, secondary lines, badges |
  | `text-sm` | 13 px | controls, buttons, sidebar items |
  | `text-base` | 14 px | body and table cells |
  | `text-h2` | 15 px | card and block headings |
  | `text-h1` | 24 px | page titles |

- **FE-93** Sizes are defined in rem, with 16 px as 1 rem, so text grows with the browser's font size setting. The px values in this file are the equivalents at the default size.

### Density and shape

- **FE-94** Every spacing and size token is a multiple of 4 px.
- **FE-95** A table row is 48 px high and the header row 40 px, with 12 px cell padding. Controls are 36 px high and small buttons 28 px.
- **FE-96** Photos are 36 px in Merchant SKU rows, 40 px in SPU rows and 112 px on the SPU page.
- **FE-97** Corner radius is 6 px on controls, 8 px on cards and tables, and fully round on badges. Card padding is 16 px, the gap between cards 16 px, and the sidebar 240 px wide.
- **FE-98** There are no gradients and no heavy shadows.

### Palette

- **FE-99** v1 is light only. The tokens are named by role and not by colour, so a dark set can be added later without touching a component.

  | Token | Value | Use |
  | --- | --- | --- |
  | `bg` | `#f6f7f9` | page |
  | `surface` | `#ffffff` | cards, tables, sidebar |
  | `surface-2` | `#f1f3f6` | SPU group rows, hover, restock working |
  | `border` | `#dde1e7` | cards, tables, dividers |
  | `input-border` | `#7d8796` | inputs, selects and other control edges |
  | `ink` | `#17202b` | text |
  | `muted` | `#566170` | secondary text |
  | `primary` / `primary-ink` | `#0f6e66` / `#ffffff` | primary buttons, active tab, focus ring |
  | `link` | `#0b6159` | links |
  | `nav-active` / `nav-active-ink` | `#e3f1ef` / `#0b544d` | current sidebar item |
  | `late` / `late-bg` / `late-line` | `#a61b1b` / `#fdecec` / `#f0c2c2` | Late, Stale, errors |
  | `now` / `now-bg` / `now-line` | `#8a4b00` / `#fff3dc` / `#efd096` | Order now, warnings |
  | `fine` / `fine-bg` / `fine-line` | `#1d6b3a` / `#e7f4eb` / `#b7dcc3` | Fine, Fresh, success |

- **FE-100** Every pair of text and background reaches at least 4.5:1.
- **FE-101** `border` is for cards, tables and dividers only. The edge of a control uses `input-border`, which reaches the 3:1 that WCAG 1.4.11 asks of a control's edge.
- **FE-102** Every focusable element shows a solid 2 px outline in `primary` with a 2 px offset when it has keyboard focus.

### Restock levels and other states

- **FE-103** A restock level is a soft badge with its own Lucide icon and the word always shown, so it reads without colour:

  | Level | Icon | Colours |
  | --- | --- | --- |
  | Late | `octagon-alert` | `late` |
  | Order now | `triangle-alert` | `now` |
  | Fine | `circle-check` | `fine` |

- **FE-104** Data freshness uses the same badges: Stale takes Late's and Fresh takes Fine's.
- **FE-105** The same icons mark the severity of a "needs attention" item, with the `info` icon for the rest.

Not chosen: a warm stone and indigo direction with 60 px rows, a zinc direction with a three-step meter for the levels (only its typeface was kept), IBM Plex Sans, and a dark mode in v1.

_Sources: [Visual design: palette, typeface and density](https://github.com/byanto/niskora/issues/32), [Main screens and navigation](https://github.com/byanto/niskora/issues/25), the `prototype/visual-design` branch._

## 12. Window width and zoom

- **FE-106** Screens are designed and judged at 1280 px and wider. Tablet and phone layouts are out of scope for v1.
- **FE-107** Below 1024 px the sidebar collapses into a menu button that opens it as a drawer, and page headers, actions, filters and card grids wrap into one column. The aim there is "usable and accessible", not a design for a small device.
- **FE-108** The page itself never scrolls sideways, down to 320 px wide, which is 1280 px at 400% zoom (WCAG Reflow). A data table scrolls sideways inside its own box, which Reflow allows.
- **FE-109** Every screen is checked at four widths: 1280 px and 1440 px for the design, 1024 px for a scaled laptop screen, and 320 px for Reflow.

_Sources: [Visual design: palette, typeface and density](https://github.com/byanto/niskora/issues/32), [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [Main screens and navigation](https://github.com/byanto/niskora/issues/25)._

## 13. Tests and agent skills

The test layers, the Playwright journeys and the browser check by the coding agent are in [engineering.md](engineering.md). These rules add what is particular to screens.

- **FE-110** Accessibility is checked with axe in the screen tests and in the Playwright journeys.
- **FE-111** Build sessions may load the `frontend-ui-engineering` skill from `addyosmani/agent-skills`. A `frontend/CLAUDE.md` states the niskora rules that win over any skill: designed for desktop and not mobile first, no optimistic updates, Problem Details errors and no Storybook.

_Sources: [Frontend architecture and libraries](https://github.com/byanto/niskora/issues/26), [Visual design: palette, typeface and density](https://github.com/byanto/niskora/issues/32), [Engineering conventions: testing, CI and migrations](https://github.com/byanto/niskora/issues/14)._
