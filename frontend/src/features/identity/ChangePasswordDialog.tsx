import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery } from "@tanstack/react-query";
import KeyRound from "lucide-react/icons/key-round";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { z } from "zod";
import { fieldErrorsOf } from "@/lib/api/problem";
import { ActionFailed } from "@/ui/action-failed";
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
import { Field } from "@/ui/field";
import { Loading, Skeleton } from "@/ui/skeleton";
import { toast } from "@/ui/toaster";
import { ChangeProblem } from "./ChangeProblem";
import { useAccountChange } from "./useAccountChange";
import { changeOwnPassword, type User, type UserId, userQuery } from "./users";

const fields = ["currentPassword", "newPassword"] as const;
type FieldName = (typeof fields)[number];

function isFieldName(name: string): name is FieldName {
  return (fields as readonly string[]).includes(name);
}

/**
 * "Change password" in the sidebar (PLT-17, FE-8): a dialog that asks for the logged-in user's
 * current password and a new one. It reads their account first, for the version the change is
 * based on (ENG-87), and closes once the server has changed it, with a toast (FE-54).
 */
export function ChangePasswordDialog({ userId }: { userId: UserId }) {
  const { t } = useTranslation("identity");
  const [open, setOpen] = useState(false);
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <KeyRound aria-hidden="true" />
          {t("changePassword.open")}
        </Button>
      </DialogTrigger>
      <DialogContent closeLabel={t("changePassword.close")}>
        <DialogHeader>
          <DialogTitle>{t("changePassword.title")}</DialogTitle>
          <DialogDescription>{t("changePassword.description")}</DialogDescription>
        </DialogHeader>
        {/* Mounted while open only, so each opening reads the account afresh. */}
        <OwnAccount
          userId={userId}
          onDone={() => {
            setOpen(false);
          }}
        />
      </DialogContent>
    </Dialog>
  );
}

function OwnAccount({ userId, onDone }: { userId: UserId; onDone: () => void }) {
  const { t: tCommon } = useTranslation();
  const account = useQuery({ ...userQuery(userId), staleTime: 0 });
  if (account.isPending) {
    return (
      <Loading>
        <div className="flex flex-col gap-4">
          <Skeleton className="h-control w-full" />
          <Skeleton className="h-control w-full" />
        </div>
      </Loading>
    );
  }
  if (account.isError) {
    return (
      <div className="flex flex-col gap-4">
        <ActionFailed error={account.error} />
        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => {
              void account.refetch();
            }}
          >
            {tCommon("loadFailed.retry")}
          </Button>
        </DialogFooter>
      </div>
    );
  }
  return <ChangePasswordForm user={account.data} onDone={onDone} />;
}

function ChangePasswordForm({ user, onDone }: { user: User; onDone: () => void }) {
  const { t } = useTranslation("identity");
  const { t: tCommon } = useTranslation();
  // Zod checks only that each is given; the backend decides the rest (FE-63).
  const schema = useMemo(
    () =>
      z.object({
        currentPassword: z.string().min(1, t("changePassword.currentPasswordRequired")),
        newPassword: z.string().min(1, t("changePassword.newPasswordRequired")),
      }),
    [t],
  );
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: { currentPassword: "", newPassword: "" },
  });
  const change = useAccountChange({
    user,
    change: changeOwnPassword,
    onChanged: () => {
      onDone();
      toast.success(t("changePassword.done"));
    },
    onFailed: (error) => {
      for (const { field, key } of fieldErrorsOf(error)) {
        if (isFieldName(field)) {
          form.setError(field, { type: "server", message: tCommon(`fieldErrors.${key}`) });
        }
      }
    },
  });
  // Field errors show on their fields; any other failure as an alert above the buttons (FE-55).
  const fieldErrors = fieldErrorsOf(change.error);
  const onlyFieldErrors =
    fieldErrors.length > 0 && fieldErrors.every(({ field }) => isFieldName(field));

  return (
    <form
      noValidate
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        void form.handleSubmit((values) => {
          change.save(values);
        })(event);
      }}
    >
      <Field
        label={t("changePassword.currentPassword")}
        error={form.formState.errors.currentPassword}
        input={form.register("currentPassword")}
        type="password"
        autoComplete="current-password"
        autoFocus
      />
      <Field
        label={t("changePassword.newPassword")}
        error={form.formState.errors.newPassword}
        hint={t("changePassword.newPasswordHint")}
        input={form.register("newPassword")}
        type="password"
        autoComplete="new-password"
      />
      <ChangeProblem change={change} showError={!onlyFieldErrors} />
      <DialogFooter className="mt-2">
        <DialogClose asChild>
          <Button variant="outline">{t("changePassword.cancel")}</Button>
        </DialogClose>
        <Button type="submit" disabled={change.saving || change.conflict !== null}>
          {change.saving ? t("changePassword.submitting") : t("changePassword.submit")}
        </Button>
      </DialogFooter>
    </form>
  );
}
