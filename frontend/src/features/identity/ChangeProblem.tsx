import { ActionFailed } from "@/ui/action-failed";
import { VersionConflict } from "@/ui/version-conflict";
import type { useAccountChange } from "./useAccountChange";

/**
 * Why an account change did not go through, just above the dialog's buttons (FE-55): who changed
 * the account first, with the button that loads the latest version (FE-81), or any other failure
 * as an alert. Field errors show on their fields instead, so `showError` leaves them out.
 */
export function ChangeProblem({
  change,
  showError = true,
}: {
  change: ReturnType<typeof useAccountChange<never>>;
  showError?: boolean;
}) {
  if (change.conflict !== null) {
    return (
      <VersionConflict
        conflict={change.conflict}
        loading={change.loadingLatest}
        onLoadLatest={change.loadLatest}
      />
    );
  }
  if (change.loadLatestError !== null) {
    return <ActionFailed error={change.loadLatestError} />;
  }
  if (change.error !== null && showError) {
    return <ActionFailed error={change.error} />;
  }
  return null;
}
