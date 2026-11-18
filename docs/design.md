# Design system

The look of niskora is variant D of the visual design prototype: cool slate neutrals, a deep teal accent, Geist type, light only. [docs/spec/frontend.md](spec/frontend.md) section 11 decides it. This file is how to build with it.

## Where it lives

| What | Where |
| --- | --- |
| Tokens: colours, type sizes, spacing, named sizes, radii | the `@theme` block of `frontend/src/index.css` |
| Base styles: page colours, card edges, keyboard focus | the `@layer base` block of the same file |
| shadcn/ui components, restyled to the tokens | `frontend/src/ui/` |
| Class merging that knows the token names | `cn()` in `frontend/src/lib/utils.ts` |
| Checks | `frontend/palette.test.ts` (contrast) and the design token rules in `frontend/eslint.config.js` |

The theme replaces Tailwind's defaults rather than adding to them. Only niskora's tokens exist, so `bg-red-500`, `text-lg`, `rounded-xl` and `shadow-md` produce no CSS.

## Colour

Colours are named by role, never by hue (FE-99), so a dark set can replace the values later without touching a component.

- Page `bg-bg`; cards, tables and the sidebar `bg-surface`; group rows, hover and the restock working `bg-surface-2`.
- Text `text-ink`, secondary text `text-muted`, links `text-link`.
- Primary buttons and the active tab: `bg-primary text-primary-ink`. The current sidebar item: `bg-nav-active text-nav-active-ink`.
- A state uses its three tokens together: `text-late bg-late-bg border-late-line`, and the same for `now` and `fine`. `late` also marks errors, `now` warnings and `fine` success.
- A bare `border` is the light edge of cards, tables and dividers. A control's edge is `border-input-border`, which reaches the 3:1 a control needs (FE-101).
- Every text and background pair reaches 4.5:1 (FE-100). A new pair goes into `palette.test.ts` with the component that uses it.

Lint rejects a raw colour (`#0f6e66`, `rgb()`, `oklch()`) in any string in `src/` outside tests, in a class or a style. A colour that has no token yet gets one in `index.css` and a line here.

## Type

All text is Geist and codes are Geist Mono (`font-mono`), self-hosted through `@fontsource-variable` and bundled into the build (FE-91). Sizes are in rem (FE-93):

| Class | Size | Use |
| --- | --- | --- |
| `text-xs` | 12 px | labels, table headers, secondary lines, badges |
| `text-sm` | 13 px | controls, buttons, sidebar items |
| `text-base` | 14 px | body and table cells, the default |
| `text-h2` | 15 px | card and block headings |
| `text-h1` | 24 px | page titles |

Codes (Merchant SKU, SPU, Package No, purchase order numbers) and the restock working are `font-mono` (FE-62). Numbers in tables and figures are `tabular-nums` and right-aligned (FE-61).

## Spacing, sizes and shape

One spacing step is 4 px: `p-4` is 16 px, `gap-2` 8 px (FE-94). Lint rejects half steps (`gap-1.5`) and arbitrary lengths (`w-[13px]`). Arbitrary variants and grid templates (`data-[state=open]:`, `grid-cols-[1fr_auto]`) are fine.

The fixed measures of FE-95 to FE-97 are named, so a component says what it is:

| Class suffix | Size | Use |
| --- | --- | --- |
| `control` | 36 px | buttons, inputs, selects (`h-control`) |
| `control-sm` | 28 px | small buttons |
| `row` / `row-header` | 48 / 40 px | table rows and the header row |
| `cell` | 12 px | table cell padding (`px-cell`) |
| `card` | 16 px | card padding (`p-card`); cards sit `gap-4` apart |
| `photo-sku` / `photo-spu` / `photo-hero` | 36 / 40 / 112 px | photos in Merchant SKU rows, SPU rows and on the SPU page |
| `sidebar` | 240 px | the sidebar width (`w-sidebar`) |

Corners are `rounded-control` (6 px) on controls, `rounded-card` (8 px) on cards and tables, and `rounded-full` on badges. There are no gradients and no shadows (FE-98): the shadow tokens are removed.

## Focus

Every focusable element shows a solid 2 px `primary` outline with a 2 px offset on keyboard focus (FE-102). The rule is `:focus-visible` in `index.css`, so components add nothing for it. A component keeps the outline: it never sets `outline-none` or replaces it with a ring.

## States: levels, freshness and severity

A state is never shown by colour alone (FE-58). `StatusBadge` in `src/ui/status-badge.tsx` holds the one map from a state to its tone and Lucide icon, and shows the icon beside the word:

| Status | Icon | Tone | Shows |
| --- | --- | --- | --- |
| `late` | `octagon-alert` | `late` | Late, Stale, errors |
| `now` | `triangle-alert` | `now` | Order now, warnings |
| `fine` | `circle-check` | `fine` | Fine, Fresh, success |
| `info` | `info` | neutral | anything else on "needs attention" |

A feature maps its own enum to a status and passes the translated word, as `RestockLevelBadge` (`features/restock`) and `FreshnessBadge` (`app/`) do. `StatusIcon` marks the severity of a "needs attention" item (FE-105) beside text that names it.

## Components and icons

Components are shadcn/ui, copied into `src/ui/` and then made niskora's own (FE-45). To add one, run `pnpm dlx shadcn@<version> add <name>` in `frontend/`, then:

1. Change `import { cn } from "cn"` to `import { cn } from "@/lib/utils"` and remove the `cn` package the CLI added to `package.json`. niskora's `cn()` knows the token names; the package's would drop `text-h1` beside `text-ink`.
2. Import Radix primitives one at a time: `import * as Slot from "radix-ui/slot"`, never from `radix-ui`.
3. Replace shadcn's tokens with niskora's: `foreground` becomes `ink`, `muted-foreground` becomes `muted`, `input` becomes `input-border`, `destructive` becomes `late`, and so on. Remove `shadow-*`, `ring-*`, `outline-none` and `dark:` classes and any size off the 4 px grid.
4. Run `pnpm lint`: it rejects whatever is left of shadcn's look.

Icons come from Lucide only, one module per icon: `import Info from "lucide-react/icons/info"`. The `lucide-react` index loads all of Lucide and makes every test slow, so lint rejects it except for `import type`.
