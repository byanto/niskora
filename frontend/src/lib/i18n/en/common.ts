/** English text for the `common` module and the parts of the app that belong to no module. */
export const common = {
  appName: "niskora",
  nav: {
    label: "Main",
    open: "Open menu",
    close: "Close menu",
    drawer: "Menu",
    groups: {
      daily: "Daily",
      catalogue: "Catalogue",
      buying: "Buying",
      analysis: "Analysis",
      setup: "Setup",
    },
    screens: {
      home: "Home",
      imports: "Imports",
      users: "Users",
    },
  },
  loading: "Loading…",
  /**
   * The message of each error code the backend sends (FE-56), and of the two failures that carry
   * none: no answer at all (`NETWORK`) and anything else (`UNEXPECTED`).
   */
  errors: {
    BAD_REQUEST: "The request was not understood. Reload the page, then try again.",
    VALIDATION_FAILED: "Some fields need a change.",
    IDEMPOTENCY_KEY_MISSING: "The request was not understood. Reload the page, then try again.",
    IDEMPOTENCY_KEY_REUSED: "This was already sent with other values. Try again.",
    REQUEST_IN_PROGRESS: "This is still being saved. Wait a moment, then check.",
    UNAUTHORIZED: "You are not logged in.",
    BAD_CREDENTIALS: "The login name or password is wrong.",
    TOO_MANY_LOGIN_ATTEMPTS: "Too many failed logins. Wait a moment, then try again.",
    CSRF_TOKEN_INVALID: "This page is out of date. Reload it, then try again.",
    NOT_FOUND: "This was not found. It may have been removed.",
    VERSION_MISMATCH: "Someone changed this after you opened it. Reload the page, then try again.",
    IF_MATCH_REQUIRED: "The request was not understood. Reload the page, then try again.",
    IF_MATCH_INVALID: "The request was not understood. Reload the page, then try again.",
    ACCOUNT_DISABLED: "This account is disabled. Ask another user to enable it.",
    CANNOT_DISABLE_OWN_ACCOUNT: "You cannot disable your own account.",
    CANNOT_RESET_OWN_PASSWORD: "Change your own password from the sidebar.",
    CAN_ONLY_CHANGE_OWN_PASSWORD: "You can only change your own password. Reset theirs instead.",
    INTERNAL_SERVER_ERROR: "Something went wrong on the server.",
    BAD_GATEWAY: "The server is not answering. Wait a moment.",
    SERVICE_UNAVAILABLE: "The server is not ready. Wait a moment.",
    GATEWAY_TIMEOUT: "The server is not answering. Wait a moment.",
    NETWORK: "The server could not be reached. Check the connection.",
    UNEXPECTED: "Something went wrong.",
  },
  /** The message of each field error code the backend sends (FE-56), and `INVALID` for the rest. */
  fieldErrors: {
    REQUIRED: "Fill this in.",
    MUST_BE_ONE_WORD: "Use one word, with no spaces.",
    TOO_SHORT: "This is too short.",
    LOGIN_NAME_TAKEN: "Another user has this login name.",
    WRONG_PASSWORD: "This is not your current password.",
    INVALID: "This is not valid.",
  },
  /** A change refused because someone else changed the thing first (FE-81). */
  conflict: {
    changedBy: "{{fullName}} changed this at {{moment}}, after you opened it.",
    changed: "This was changed at {{moment}}, after you opened it.",
    loadLatest: "Load the latest version",
    loading: "Loading…",
  },
  loadFailed: {
    title: "This could not be loaded",
    retry: "Try again",
  },
  placeholder: {
    title: "Not built yet",
    description: "This screen arrives in a later version of niskora.",
  },
  newVersion: {
    message: "A new version is available,",
    reload: "reload",
  },
  home: {
    title: "Home",
    empty: {
      title: "Nothing imported yet",
      description:
        "Home shows data freshness, what needs attention, restock advice, purchase orders and yesterday's demand once the first BigSeller exports are imported.",
      action: "Import BigSeller exports",
    },
  },
};
