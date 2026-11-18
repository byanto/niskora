# niskora frontend

The spec for screens is `docs/spec/frontend.md`. Build the look with `docs/design.md`: read it before you write or restyle a component.

## niskora rules that win over any skill

These hold whatever a skill such as `frontend-ui-engineering` says (FE-111):

- **Desktop first.** Screens are designed and judged at 1280 px and wider; below 1024 px they only need to be usable and accessible (FE-106 to FE-109).
- **The server answers first.** A save shows that it is saving, and the screen changes only after the server answers. There are no optimistic updates (FE-54).
- **Errors are Problem Details** (RFC 9457). Each error code maps to a translation key, and a field error lands on its field by its JSON Pointer (FE-53, FE-56).
- **Screen tests are the catalogue.** There is no Storybook (FE-48): a component is shown and checked in Vitest with axe (FE-110).
