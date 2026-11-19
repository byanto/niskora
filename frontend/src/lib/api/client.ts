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

// Every response names the backend's build, so the shell can offer a reload (FE-82).
api.use({
  onResponse({ response }) {
    noteBackendBuild(response.headers.get(BUILD_ID_HEADER));
  },
});
