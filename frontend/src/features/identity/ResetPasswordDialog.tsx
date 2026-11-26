import { zodResolver } from "@hookform/resolvers/zod";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { z } from "zod";
import { fieldErrorsOf } from "@/lib/api/problem";
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
import { toast } from "@/ui/toaster";
import { ChangeProblem } from "./ChangeProblem";
import { useAccountChange } from "./useAccountChange";
import { resetPassword, type User } from "./users";

/**
 * "Reset password" on another user's row (PLT-16, FE-37): a dialog that asks for their new
 * password. It closes once the server has changed it, and a toast says so (FE-54, FE-55).
 */
export function ResetPasswordDialog({ user }: { user: User }) {
  const { t } = useTranslation("identity");
  const [open, setOpen] = useState(false);
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          {t("users.reset.open")}
        </Button>
      </DialogTrigger>
      <DialogContent closeLabel={t("users.reset.close")}>
        {/* Mounted while open only, so each opening starts from the row as it is listed. */}
        <ResetPasswordForm
          user={user}
          onDone={() => {
            setOpen(false);
          }}
        />
      </DialogContent>
    </Dialog>
  );
}

function ResetPasswordForm({ user, onDone }: { user: User; onDone: () => void }) {
  const { t } = useTranslation("identity");
  const { t: tCommon } = useTranslation();
  // Zod checks only that it is given; the backend decides the rest (FE-63).
  const schema = useMemo(
    () => z.object({ password: z.string().min(1, t("users.reset.passwordRequired")) }),
    [t],
  );
  const form = useForm({ resolver: zodResolver(schema), defaultValues: { password: "" } });
  const change = useAccountChange({
    user,
    change: resetPassword,
    onChanged: () => {
      onDone();
      toast.success(t("users.reset.done", { fullName: user.fullName }));
    },
    onFailed: (error) => {
      for (const { field, key } of fieldErrorsOf(error)) {
        if (field === "password") {
          form.setError(field, { type: "server", message: tCommon(`fieldErrors.${key}`) });
        }
      }
    },
  });
  // Field errors show on their field; any other failure as an alert above the buttons (FE-55).
  const fieldErrors = fieldErrorsOf(change.error);
  const onlyFieldErrors =
    fieldErrors.length > 0 && fieldErrors.every(({ field }) => field === "password");

  return (
    <>
      <DialogHeader>
        <DialogTitle>{t("users.reset.title", { fullName: user.fullName })}</DialogTitle>
        <DialogDescription>{t("users.reset.description")}</DialogDescription>
      </DialogHeader>
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
          label={t("users.reset.password")}
          error={form.formState.errors.password}
          hint={t("users.reset.passwordHint")}
          input={form.register("password")}
          type="password"
          autoComplete="new-password"
          autoFocus
        />
        <ChangeProblem change={change} showError={!onlyFieldErrors} />
        <DialogFooter className="mt-2">
          <DialogClose asChild>
            <Button variant="outline">{t("users.reset.cancel")}</Button>
          </DialogClose>
          <Button type="submit" disabled={change.saving || change.conflict !== null}>
            {change.saving ? t("users.reset.submitting") : t("users.reset.submit")}
          </Button>
        </DialogFooter>
      </form>
    </>
  );
}
