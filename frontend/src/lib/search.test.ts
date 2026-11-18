import { describe, expect, expectTypeOf, it } from "vitest";
import { pageSearch } from "./search";

// The router hands validateSearch the search parameters already parsed from the URL, so
// `?page=3` arrives as the number 3 and `?page=abc` as the string "abc".
describe("pageSearch", () => {
  it("reads the page number", () => {
    expect(pageSearch.parse({ page: 3 })).toEqual({ page: 3 });
  });

  it("starts at page 1 when the URL names none", () => {
    expect(pageSearch.parse({})).toEqual({ page: 1 });
  });

  it.each([["abc"], [0], [-2], [2.5]])("falls back to page 1 for %j", (page) => {
    expect(pageSearch.parse({ page })).toEqual({ page: 1 });
  });

  it("types the page as a number", () => {
    expectTypeOf(pageSearch.parse({})).toEqualTypeOf<{ page: number }>();
  });
});
