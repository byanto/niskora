/**
 * Two users editing the same thing (FE-81). Every editable resource comes with its version, and
 * every change sends back the version it was based on as `If-Match`, in the backend's `ETag` form:
 * `"3"` (ENG-87). The frontend takes the version from the body, where a list has it too.
 */
export function ifMatch(version: number): string {
  return `"${String(version)}"`;
}
