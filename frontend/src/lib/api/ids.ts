declare const kind: unique symbol;

/**
 * An id from the API, branded with the kind of thing it names (FE-44, ENG-75), so a supplier id
 * cannot be passed where a purchase order id is expected. At runtime it is the plain string the
 * API sent: a UUID for a thing niskora creates (ENG-71).
 *
 * Each module names its own: `type SupplierId = Id<"Supplier">`.
 */
export type Id<Kind extends string> = string & { readonly [kind]: Kind };

/**
 * Brands a string from the API as an id. The query definitions call it where a response enters
 * the app; nothing else makes an id.
 */
export function toId<Kind extends string>(value: string): Id<Kind> {
  return value as Id<Kind>;
}
