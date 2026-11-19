import { useSyncExternalStore } from "react";

/** The header in which every response carries the backend's build id (ENG-92). */
export const BUILD_ID_HEADER = "Niskora-Build-Id";

/**
 * The build this frontend was made from. The image build sets it to the commit hash, the same
 * value the backend's image gets; a local build is `dev`, as a local backend is.
 */
const FRONTEND_BUILD_ID = import.meta.env.VITE_BUILD_ID ?? "dev";

let newVersion = false;
const listeners = new Set<() => void>();

/**
 * Notes the build id a response came from. Once it differs from this frontend's build, a new
 * version is running and stays so until the page is reloaded (FE-82). A response without the
 * header, such as an error from the proxy, says nothing about the version.
 */
export function noteBackendBuild(buildId: string | null) {
  if (buildId === null || buildId === FRONTEND_BUILD_ID || newVersion) {
    return;
  }
  newVersion = true;
  for (const listener of listeners) {
    listener();
  }
}

/** Whether the backend answers from a newer build than this page was loaded with. */
export function useNewVersionAvailable(): boolean {
  return useSyncExternalStore(subscribe, () => newVersion);
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** Forgets what was noted, so each test starts from a page that has just loaded. */
export function resetBuildWatch() {
  newVersion = false;
}
