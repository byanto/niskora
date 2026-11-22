import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "@/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/ui/dialog";
import { toast } from "@/ui/toaster";
import { ChangeProblem } from "./ChangeProblem";
import { useAccountChange } from "./useAccountChange";
import { disableUser, enableUser, type User } from "./users";

type Access = "disable" | "enable";

/**
 * "Disable" or "Enable" on another user's row (PLT-21, PLT-45, FE-37): a dialog that says what
 * the change does and asks to confirm it. It closes once the server has made the change, and a
 * toast says so (FE-54, FE-55).
 */
export function AccountAccessDialog({ user, access }: { user: User; access: Access }) {
  const { t } = useTranslation("identity");
  const [open, setOpen] = useState(false);
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {/* One width for "Disable" and "Enable", so the buttons line up from row to row. */}
        <Button variant="outline" size="sm" className="w-20">
          {t(`users.${access}.open`)}
        </Button>
      </DialogTrigger>
      <DialogContent closeLabel={t("users.access.close")}>
        {/* Mounted while open only, so each opening starts from the row as it is listed. */}
        <AccountAccessForm
          user={user}
          access={access}
          onDone={() => {
            setOpen(false);
          }}
        />
      </DialogContent>
    </Dialog>
  );
}

function AccountAccessForm({
  user,
  access,
  onDone,
}: {
  user: User;
  access: Access;
  onDone: () => void;
}) {
  const { t } = useTranslation("identity");
  const change = useAccountChange({
    user,
    change: access === "disable" ? disableUser : enableUser,
    onChanged: () => {
      onDone();
      toast.success(t(`users.${access}.done`, { fullName: user.fullName }));
    },
  });
  // The latest version may already be what was asked, when someone else made the same change.
  const alreadyDone = change.current.isDisabled === (access === "disable");
  const fullName = user.fullName;

  return (
    <>
      <DialogHeader>
        <DialogTitle>{t(`users.${access}.title`, { fullName })}</DialogTitle>
        <DialogDescription>
          {alreadyDone
            ? t(`users.${access}.already`, { fullName })
            : t(`users.${access}.description`)}
        </DialogDescription>
      </DialogHeader>
      <form
        noValidate
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault();
          change.save(undefined);
        }}
      >
        <ChangeProblem change={change} />
        <DialogFooter className="mt-2">
          {alreadyDone ? (
            <DialogClose asChild>
              <Button>{t("users.access.done")}</Button>
            </DialogClose>
          ) : (
            <>
              <DialogClose asChild>
                <Button variant="outline">{t("users.access.cancel")}</Button>
              </DialogClose>
              <Button type="submit" disabled={change.saving || change.conflict !== null}>
                {change.saving ? t(`users.${access}.submitting`) : t(`users.${access}.submit`)}
              </Button>
            </>
          )}
        </DialogFooter>
      </form>
    </>
  );
}
