# Received quantities are read from BigSeller's stock movements, never typed into niskora

niskora owns the purchase order, yet it has no receiving screen. When goods arrive they are counted and the count is entered once, in BigSeller, as BigSeller's own purchase document. The next daily stock movement import brings the Purchase Stock-In rows, niskora proposes the placed purchase order whose open lines hold those Single SKUs, and a user confirms the match. Only then does niskora fill in received quantities, close lines and show shortfalls and overages. We chose this because BigSeller is the source of truth for stock, so the count must be entered there in any case, and a second entry in niskora would be double work that can disagree with it. It also keeps on order in step with stock: the same import that raises available stock lowers on order, so restock advice never counts arriving units twice or loses them for a day.

BigSeller's stock movement carries only its own `PO...` document number and no note, so a niskora purchase order number cannot travel through BigSeller. That is why the link is a proposed match confirmed by a user, and why one purchase order can be matched to several BigSeller documents, one per delivery.

## Considered options

- **A receiving screen in niskora, with the count typed there and again in BigSeller:** niskora would know about a delivery the same day. Rejected because of the double entry, and because on order would drop a day before the imported stock rises, making restock advice ask for goods that have just arrived.
- **A receiving screen in niskora that produces a file for BigSeller:** one entry, made in niskora. Rejected because stock would then depend on a file being carried over correctly every time, which moves niskora from complementing BigSeller towards replacing part of it.
- **Linking by a number the user types:** the user copies BigSeller's `PO...` number onto the niskora purchase order. Rejected as the main route because the proposed match needs no typing; nothing prevents a user from picking the purchase order by hand when the proposal is wrong.

## Consequences

- Purchasing figures are as old as the last stock movement import. A delivery shows in niskora the day after it is entered in BigSeller.
- A Purchase Stock-In that fits no purchase order is kept as an unmatched stock-in until a user matches or dismisses it.
- A gap in the stock movement imports delays receiving, so the staleness warning for that data set also guards purchasing.
- niskora produces a BigSeller import file from a placed purchase order so the lines are not retyped into BigSeller's draft. That file flows towards BigSeller before the count and is a convenience only: nothing in niskora depends on it having been used.
