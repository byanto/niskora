import createClient from "openapi-fetch";
import { BUILD_ID_HEADER, noteBackendBuild } from "./build";
import type { paths } from "./schema";

/**
 * The one client for the backend (FE-43), typed by the types generated from the committed
 * OpenAPI document. Query definitions call it, per module; a component never does.
 *
 * The API is on the app's own origin (FE-83). `fetch` is looked up on every call rather than
 * captured once, so the fake network of the tests (ENG-38) sees every request.
 */
export const api = createClient<paths>({
  baseUrl: globalThis.location.origin,
  fetch: (request) => globalThis.fetch(request),
});

/** The cookie in which Spring Security hands out the CSRF token, and the header it expects back. */
const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";

/** The methods that change nothing, and so carry no CSRF token. */
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

// Every changing request echoes the CSRF token cookie in a header (FE-73). The session cookie
// itself is HttpOnly, so the browser sends it and nothing here sees it (FE-74).
api.use({
  onRequest({ request }) {
    const token = cookie(CSRF_COOKIE);
    if (!SAFE_METHODS.has(request.method) && token !== undefined) {
      request.headers.set(CSRF_HEADER, token);
    }
    return request;
  },
});

// Every response names the backend's build, so the shell can offer a reload (FE-82).
api.use({
  onResponse({ response }) {
    noteBackendBuild(response.headers.get(BUILD_ID_HEADER));
  },
});

function cookie(name: string): string | undefined {
  for (const pair of document.cookie.split("; ")) {
    const separator = pair.indexOf("=");
    if (pair.slice(0, separator) === name && separator < pair.length - 1) {
      return decodeURIComponent(pair.slice(separator + 1));
    }
  }
  return undefined;
}
