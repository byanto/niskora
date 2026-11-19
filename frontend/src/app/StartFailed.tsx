import { useRouter } from "@tanstack/react-router";
import { LoadFailed } from "@/ui/load-failed";

/**
 * The app could not ask who is logged in (FE-71), so it can show neither the shell nor Login: the
 * translated reason with a retry, alone on the page.
 */
export function StartFailed({ error }: { error: unknown }) {
  const router = useRouter();
  return (
    <main className="flex min-h-screen items-center justify-center p-4">
      <LoadFailed
        className="w-full max-w-md"
        error={error}
        onRetry={() => {
          void router.invalidate();
        }}
      />
    </main>
  );
}
