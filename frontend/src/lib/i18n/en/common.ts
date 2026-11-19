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
    },
  },
  loading: "Loading…",
  /**
   * The message of each error code the backend sends (FE-56), and of the two failures that carry
   * none: no answer at all (`NETWORK`) and anything else (`UNEXPECTED`).
   */
  errors: {
    NOT_FOUND: "This was not found. It may have been removed.",
    INTERNAL_SERVER_ERROR: "Something went wrong on the server.",
    SERVICE_UNAVAILABLE: "The server is not ready. Wait a moment.",
    NETWORK: "The server could not be reached. Check the connection.",
    UNEXPECTED: "Something went wrong.",
  },
  loadFailed: {
    title: "This could not be loaded",
    retry: "Try again",
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
    },
  },
};
