import { describe, expectTypeOf, it } from "vitest";
import { type Id, toId } from "./ids";

type SupplierId = Id<"Supplier">;
type PurchaseOrderId = Id<"PurchaseOrder">;

describe("Id", () => {
  it("keeps the string the API sent", () => {
    const id: SupplierId = toId("0b9e4c2a-6f1d-4e8b-9a3c-5d7e1f2a4b6c");
    expectTypeOf(id).toExtend<string>();
  });

  it("cannot be passed where an id of another kind is expected", () => {
    expectTypeOf<SupplierId>().not.toExtend<PurchaseOrderId>();
    expectTypeOf<PurchaseOrderId>().not.toExtend<SupplierId>();
  });

  it("cannot be made from a plain string by assignment", () => {
    expectTypeOf<string>().not.toExtend<SupplierId>();
  });
});
