import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import UserPlus from "lucide-react/icons/user-plus";
import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { z } from "zod";
import { fieldErrorsOf } from "@/lib/api/problem";
import { newIdempotencyKey, retryRepeated } from "@/lib/api/repeat";
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
import { toast } from "@/ui/toaster";
import { addUser, allUsersKey } from "./users";

const fields = ["fullName", "loginName", "password"] as const;
type FieldName = (typeof fields)[number];

const emptyForm = { fullName: "", loginName: "", password: "" };

function isFieldName(name: string): name is FieldName {
  return (fields as readonly string[]).includes(name);
}

/**
 * "Add user" on the Users screen (PLT-16, FE-37): a dialog that asks for the new user's full name,
 * login name and first password. It closes once the server has added them and the list shows
 * them, and a toast says so (FE-54, FE-55).
 */
export function AddUserDialog() {
  const { t } = useTranslation("identity");
  const { t: tCommon } = useTranslation();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);

  // Zod checks only that each is given; the backend decides the rest (FE-63).
  const schema = useMemo(
    () =>
      z.object({
        fullName: z.string().trim().min(1, t("users.add.fullNameRequired")),
        loginName: z.string().trim().min(1, t("users.add.loginNameRequired")),
        password: z.string().min(1, t("users.add.passwordRequired")),
      }),
    [t],
  );
  const form = useForm({ resolver: zodResolver(schema), defaultValues: emptyForm });

  const add = useMutation({
    mutationFn: addUser,
    // A press that got no answer is sent again with its own key, so it adds the user once (FE-80).
    ...retryRepeated,
    onSuccess: async (user) => {
      // The list shows the new user before the dialog closes: the screen changes once, after the
      // server has answered (FE-54).
      await queryClient.invalidateQueries({ queryKey: allUsersKey });
      close();
      toast.success(t("users.add.added", { fullName: user.fullName }));
    },
    onError: (error) => {
      for (const { field, key } of fieldErrorsOf(error)) {
        if (isFieldName(field)) {
          form.setError(field, { type: "server", message: tCommon(`fieldErrors.${key}`) });
        }
      }
    },
  });

  // Field errors show on their fields; any other failure as an alert above the buttons (FE-55).
  const fieldErrors = fieldErrorsOf(add.error);
  const onlyFieldErrors =
    fieldErrors.length > 0 && fieldErrors.every(({ field }) => isFieldName(field));

  function close() {
    setOpen(false);
    form.reset(emptyForm);
    add.reset();
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(opening) => {
        if (opening) {
          setOpen(true);
        } else {
          close();
        }
      }}
    >
      <DialogTrigger asChild>
        <Button>
          <UserPlus aria-hidden="true" />
          {t("users.add.open")}
        </Button>
      </DialogTrigger>
      <DialogContent closeLabel={t("users.add.close")}>
        <DialogHeader>
          <DialogTitle>{t("users.add.title")}</DialogTitle>
          <DialogDescription>{t("users.add.description")}</DialogDescription>
        </DialogHeader>
        <form
          noValidate
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            void form.handleSubmit((user) => {
              // One key per press of the button (FE-80).
              add.mutate({ user, idempotencyKey: newIdempotencyKey() });
            })(event);
          }}
        >
          <Field
            label={t("users.add.fullName")}
            error={form.formState.errors.fullName}
            input={form.register("fullName")}
            autoComplete="off"
            autoFocus
          />
          <Field
            label={t("users.add.loginName")}
            error={form.formState.errors.loginName}
            hint={t("users.add.loginNameHint")}
            input={form.register("loginName")}
            autoComplete="off"
            spellCheck={false}
          />
          <Field
            label={t("users.add.password")}
            error={form.formState.errors.password}
            hint={t("users.add.passwordHint")}
            input={form.register("password")}
            type="password"
            autoComplete="new-password"
          />
          {add.isError && !onlyFieldErrors ? <ActionFailed error={add.error} /> : null}
          <DialogFooter className="mt-2">
            <DialogClose asChild>
              <Button variant="outline">{t("users.add.cancel")}</Button>
            </DialogClose>
            <Button type="submit" disabled={add.isPending}>
              {add.isPending ? t("users.add.submitting") : t("users.add.submit")}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
