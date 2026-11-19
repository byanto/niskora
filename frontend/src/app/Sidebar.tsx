import { Link } from "@tanstack/react-router";
import { useId } from "react";
import { useTranslation } from "react-i18next";
import { navigation } from "./navigation";

/**
 * The sidebar's content: the app's name and the screens grouped by job (FE-2), with the current
 * screen marked. The shell shows it fixed on the left, or in a drawer below 1024 px (FE-107).
 */
export function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { t } = useTranslation();
  const id = useId();
  return (
    <div className="flex h-full flex-col bg-surface">
      <p className="flex h-16 shrink-0 items-center px-6 text-h2 font-semibold">{t("appName")}</p>
      <nav aria-label={t("nav.label")} className="flex flex-col gap-6 overflow-y-auto px-3 pb-6">
        {navigation
          .filter(({ items }) => items.length > 0)
          .map(({ group, items }) => (
            <div key={group} className="flex flex-col gap-1">
              <p id={`${id}-${group}`} className="px-3 text-xs font-semibold text-muted">
                {t(`nav.groups.${group}`)}
              </p>
              <ul aria-labelledby={`${id}-${group}`} className="flex flex-col gap-1">
                {items.map(({ screen, to, icon: Icon }) => (
                  <li key={screen}>
                    <Link
                      to={to}
                      activeOptions={{ exact: to === "/" }}
                      onClick={onNavigate}
                      className="flex h-control items-center gap-3 rounded-control px-3 text-sm font-medium text-ink hover:bg-surface-2 aria-[current=page]:bg-nav-active aria-[current=page]:text-nav-active-ink"
                    >
                      <Icon aria-hidden="true" className="size-4 shrink-0" />
                      {t(`nav.screens.${screen}`)}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
      </nav>
    </div>
  );
}
