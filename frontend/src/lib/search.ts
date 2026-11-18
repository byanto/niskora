import { z } from "zod";

/**
 * Search parameters are typed and validated in the URL (FE-41). A route declares them with
 * `validateSearch`, which takes a Zod schema as it is. Each field ends in `.catch()`, so a
 * hand-edited or outdated link opens the screen with a sensible value instead of failing.
 *
 * The page of a paged list, starting at 1 (ENG-77, FE-59). A table route extends it with its own
 * filters and sort: `validateSearch: pageSearch.extend({ ... })`.
 */
export const pageSearch = z.object({
  page: z.int().min(1).default(1).catch(1),
});
