import "i18next";
import type { defaultNS } from ".";
import type { en } from "./en";

// Typed keys (FE-64): `t("status.up")` compiles, a key that is not in the English text does not.
declare module "i18next" {
  interface CustomTypeOptions {
    defaultNS: typeof defaultNS;
    resources: typeof en;
    strictKeyChecks: true;
  }
}
