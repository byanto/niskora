import Info from "lucide-react/icons/info";
import { useTranslation } from "react-i18next";
import { useNewVersionAvailable } from "@/lib/api/build";
import { Button } from "@/ui/button";

/**
 * "A new version is available, reload", once the backend answers from another build (FE-82). The
 * live region is always there, so a screen reader announces the notice when it appears.
 */
export function NewVersionNotice() {
  const { t } = useTranslation();
  const newVersion = useNewVersionAvailable();
  return (
    <div role="status">
      {newVersion ? (
        <p className="flex flex-wrap items-center gap-1 border-b bg-surface-2 px-4 py-2 text-sm lg:px-6">
          <Info aria-hidden="true" className="mr-1 size-4 shrink-0 text-muted" />
          {t("newVersion.message")}
          <Button
            variant="link"
            size="sm"
            className="px-0 underline"
            onClick={() => {
              window.location.reload();
            }}
          >
            {t("newVersion.reload")}
          </Button>
        </p>
      ) : null}
    </div>
  );
}
