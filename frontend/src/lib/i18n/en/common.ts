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
    UNAUTHORIZED: "You are not logged in.",
    BAD_CREDENTIALS: "The login name or password is wrong.",
    TOO_MANY_LOGIN_ATTEMPTS: "Too many failed logins. Wait a moment, then try again.",
    CSRF_TOKEN_INVALID: "This page is out of date. Reload it, then try again.",
    NOT_FOUND: "This was not found. It may have been removed.",
    INTERNAL_SERVER_ERROR: "Something went wrong on the server.",
    BAD_GATEWAY: "The server is not answering. Wait a moment.",
    SERVICE_UNAVAILABLE: "The server is not ready. Wait a moment.",
    GATEWAY_TIMEOUT: "The server is not answering. Wait a moment.",
    NETWORK: "The server could not be reached. Check the connection.",
    UNEXPECTED: "Something went wrong.",
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
