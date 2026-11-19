import { useRouter } from "@tanstack/react-router";
import { CenteredPage } from "@/ui/centered-page";
import { LoadFailed } from "@/ui/load-failed";

/**
 * The app could not ask who is logged in (FE-71), so it can show neither the shell nor Login: the
 * translated reason with a retry, alone on the page.
 */
export function StartFailed({ error }: { error: unknown }) {
  const router = useRouter();
  return (
    <CenteredPage>
      <LoadFailed
        className="w-full max-w-md"
        error={error}
        onRetry={() => {
          void router.invalidate();
        }}
      />
    </CenteredPage>
  );
}
