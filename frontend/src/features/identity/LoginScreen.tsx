import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "@tanstack/react-router";
import { useMemo } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { z } from "zod";
import { api } from "@/lib/api/client";
import { dataOf } from "@/lib/api/problem";
import { ActionFailed } from "@/ui/action-failed";
import { Button } from "@/ui/button";
import { Card } from "@/ui/card";
import { CenteredPage } from "@/ui/centered-page";
import { Field } from "@/ui/field";
import { afterLogin, sessionQuery } from "./session";

/**
 * Login, outside the shell (FE-38): a login name and a password. Once the server has accepted
 * them it goes back to the URL the user was sent away from (FE-72).
 */
export function LoginScreen({ redirect }: { redirect: string | undefined }) {
  const { t } = useTranslation("identity");
  const { t: tCommon } = useTranslation();
  const queryClient = useQueryClient();
  const router = useRouter();

  // Zod checks only that both are given; the backend decides whether they are right (FE-63).
  const schema = useMemo(
    () =>
      z.object({
        loginName: z.string().trim().min(1, t("login.loginNameRequired")),
        password: z.string().min(1, t("login.passwordRequired")),
      }),
    [t],
  );
  const form = useForm({
    resolver: zodResolver(schema),
    defaultValues: { loginName: "", password: "" },
  });

  const logIn = useMutation({
    mutationFn: (body: z.infer<typeof schema>) =>
      dataOf(api.POST("/api/identity/session", { body })),
    onSuccess: async (session) => {
      queryClient.setQueryData(sessionQuery.queryKey, session);
      await router.navigate({ href: afterLogin(redirect), replace: true });
    },
  });

  return (
    <CenteredPage>
      <Card className="w-full max-w-sm gap-6 p-6">
        <div className="flex flex-col gap-1">
          <p className="text-h2 font-semibold text-muted">{tCommon("appName")}</p>
          <h1 className="text-h1 font-semibold">{t("login.title")}</h1>
        </div>
        <form
          noValidate
          className="flex flex-col gap-4"
          onSubmit={(event) => {
            void form.handleSubmit((values) => {
              logIn.mutate(values);
            })(event);
          }}
        >
          <Field
            label={t("login.loginName")}
            error={form.formState.errors.loginName}
            input={form.register("loginName")}
            autoComplete="username"
            autoFocus
          />
          <Field
            label={t("login.password")}
            error={form.formState.errors.password}
            input={form.register("password")}
            type="password"
            autoComplete="current-password"
          />
          {/* A wrong password, a wait or no answer: it says why, above the button, until the
              next try (FE-55, FE-56). */}
          {logIn.isError ? <ActionFailed error={logIn.error} className="mt-2" /> : null}
          <Button type="submit" disabled={logIn.isPending} className="mt-2">
            {logIn.isPending ? t("login.submitting") : t("login.submit")}
          </Button>
        </form>
      </Card>
    </CenteredPage>
  );
}
