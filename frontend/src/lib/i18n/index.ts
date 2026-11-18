import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import { en } from "./en";

/** The namespace a component reads when it names none. */
export const defaultNS = "common";

/**
 * Text comes from here and never sits in a component (FE-64). The UI is English only in v1
 * (FE-65). The translations are bundled, so init finishes before the first render.
 */
void i18n.use(initReactI18next).init({
  lng: "en",
  fallbackLng: "en",
  resources: { en },
  ns: Object.keys(en),
  defaultNS,
  initAsync: false,
  // React already escapes what it renders.
  interpolation: { escapeValue: false },
});

export { i18n };
