import { Outlet } from "@tanstack/react-router";
import Menu from "lucide-react/icons/menu";
import X from "lucide-react/icons/x";
// One module per primitive: the package index pulls in all of Radix, which slows tests and dev.
import * as Dialog from "radix-ui/dialog";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/ui/button";
import { NewVersionNotice } from "./NewVersionNotice";
import { Sidebar } from "./Sidebar";

/** Tailwind's `lg` breakpoint, from which the sidebar is fixed rather than a drawer. */
const WIDE = "(min-width: 64rem)";

/**
 * The shell around every screen except Login (FE-1). From 1024 px the sidebar is fixed on the
 * left; below that it collapses into a menu button that opens it as a drawer (FE-107).
 */
export function Shell() {
  const { t } = useTranslation();
  const [drawerOpen, setDrawerOpen] = useState(false);

  // The drawer is hidden from 1024 px, where the fixed sidebar shows. Closing it there as well
  // keeps the hidden dialog from holding focus and hiding the page from screen readers.
  useEffect(() => {
    const wide = window.matchMedia(WIDE);
    const closeWhenWide = (event: MediaQueryListEvent) => {
      if (event.matches) {
        setDrawerOpen(false);
      }
    };
    wide.addEventListener("change", closeWhenWide);
    return () => {
      wide.removeEventListener("change", closeWhenWide);
    };
  }, []);

  return (
    <div className="min-h-screen">
      <aside className="fixed inset-y-0 left-0 hidden w-sidebar border-r lg:block">
        <Sidebar />
      </aside>
      <div className="lg:pl-sidebar">
        <Dialog.Root open={drawerOpen} onOpenChange={setDrawerOpen}>
          <header className="flex h-16 items-center gap-2 border-b bg-surface px-4 lg:hidden">
            <Dialog.Trigger asChild>
              <Button variant="ghost" size="icon" aria-label={t("nav.open")}>
                <Menu aria-hidden="true" />
              </Button>
            </Dialog.Trigger>
            <p className="text-h2 font-semibold">{t("appName")}</p>
          </header>
          <Dialog.Portal>
            <Dialog.Overlay className="fixed inset-0 z-40 bg-ink/40 lg:hidden" />
            <Dialog.Content
              aria-describedby={undefined}
              className="fixed inset-y-0 left-0 z-50 w-sidebar max-w-full border-r lg:hidden"
            >
              <Dialog.Title className="sr-only">{t("nav.drawer")}</Dialog.Title>
              <Sidebar
                onNavigate={() => {
                  setDrawerOpen(false);
                }}
              />
              <Dialog.Close asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={t("nav.close")}
                  className="absolute top-3 right-3"
                >
                  <X aria-hidden="true" />
                </Button>
              </Dialog.Close>
            </Dialog.Content>
          </Dialog.Portal>
        </Dialog.Root>
        <NewVersionNotice />
        <main className="p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
