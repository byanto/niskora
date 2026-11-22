package com.niskora.identity;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.niskora.DatabaseTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Changing an account over HTTP (PLT-21, PLT-22, PLT-45): resetting another user's password,
 * disabling and enabling an account, and changing your own password, each a verb sub-path (ENG-70)
 * that names the version it is based on (ENG-87). Each test drives the API as the browser does,
 * logged in as rina, with bayu as the other user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DatabaseTest
class AccountChangesApiTest {

  private static final String USERS = UsersController.PATH;
  private static final String PASSWORD = "correct horse battery";
  private static final String BAYU_PASSWORD = "bayu's own password";
  private static final String NEW_PASSWORD = "a brand new password";

  @Autowired MockMvc mvc;

  @Autowired Users users;

  private UUID rina;
  private UUID bayu;
  private Browser rinas;

  @BeforeEach
  void logInAsRina() throws Exception {
    rina = users.create("rina", "Rina Wulandari", PASSWORD);
    bayu = users.create("bayu", "Bayu Santoso", BAYU_PASSWORD);
    rinas = logIn("rina", PASSWORD);
  }

  @Test
  void anAccountIsReadWithItsVersionAsItsETag() throws Exception {
    rinas
        .get(USERS + "/" + bayu)
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"0\""))
        .andExpect(jsonPath("$.version").value(0))
        .andExpect(jsonPath("$.isDisabled").value(false));
    rinas
        .get(USERS)
        .andExpect(jsonPath("$.data[0].version").value(0))
        .andExpect(jsonPath("$.data[0].isDisabled").value(false));
  }

  @Test
  void resettingAPasswordAnswersWithTheNewVersion() throws Exception {
    rinas
        .change(reset(bayu), password(NEW_PASSWORD), "\"0\"")
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"1\""))
        .andExpect(jsonPath("$.id").value(bayu.toString()))
        .andExpect(jsonPath("$.version").value(1));

    logInFails("bayu", BAYU_PASSWORD, "BAD_CREDENTIALS");
    logIn("bayu", NEW_PASSWORD);
  }

  @Test
  void aChangeWithoutIfMatchIsRefusedWith428() throws Exception {
    rinas
        .change(reset(bayu), password(NEW_PASSWORD), null)
        .andExpect(status().isPreconditionRequired())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("IF_MATCH_REQUIRED"));
    logIn("bayu", BAYU_PASSWORD);
  }

  @Test
  void anIfMatchThatNamesNoVersionIsABadRequest() throws Exception {
    for (String ifMatch : new String[] {"*", "W/\"0\"", "0", "\"zero\""}) {
      rinas
          .change(reset(bayu), password(NEW_PASSWORD), ifMatch)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("IF_MATCH_INVALID"));
    }
  }

  @Test
  void aChangeBasedOnAnOlderVersionIsRefusedWith412NamingWhoChangedItAndWhen() throws Exception {
    UUID sari = users.create("sari", "Sari Dewi", PASSWORD);
    logIn("sari", PASSWORD)
        .change(reset(bayu), password("sari's choice of password"), "\"0\"")
        .andExpect(status().isOk());

    rinas
        .change(reset(bayu), password(NEW_PASSWORD), "\"0\"")
        .andExpect(status().isPreconditionFailed())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VERSION_MISMATCH"))
        .andExpect(jsonPath("$.changedBy.id").value(sari.toString()))
        .andExpect(jsonPath("$.changedBy.fullName").value("Sari Dewi"))
        .andExpect(jsonPath("$.changedAt").value(matchesPattern("\\d{4}-\\d\\d-\\d\\dT.*Z")));

    // Nothing was merged: Sari's password stands.
    logInFails("bayu", NEW_PASSWORD, "BAD_CREDENTIALS");
    logIn("bayu", "sari's choice of password");
  }

  @Test
  void aRepeatOfAChangeIsAnsweredAsTheFirstWasWithItsETag() throws Exception {
    UUID key = UUID.randomUUID();
    rinas.change(reset(bayu), password(NEW_PASSWORD), "\"0\"", key).andExpect(status().isOk());

    rinas
        .change(reset(bayu), password(NEW_PASSWORD), "\"0\"", key)
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"1\""))
        .andExpect(jsonPath("$.version").value(1));
    rinas.get(USERS + "/" + bayu).andExpect(header().string("ETag", "\"1\""));
  }

  @Test
  void nobodyResetsTheirOwnPasswordSinceChangingItAsksForTheCurrentOne() throws Exception {
    rinas
        .change(reset(rina), password(NEW_PASSWORD), "\"0\"")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("CANNOT_RESET_OWN_PASSWORD"));
    logIn("rina", PASSWORD);
  }

  @Test
  void aShortPasswordIsAFieldErrorAndABlankOneABadRequest() throws Exception {
    rinas
        .change(reset(bayu), password("short"), "\"0\"")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].pointer").value("/password"))
        .andExpect(jsonPath("$.errors[0].code").value("TOO_SHORT"));
    rinas
        .change(reset(bayu), password(" "), "\"0\"")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].pointer").value("/password"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
  }

  @Test
  void anUnknownAccountIsNotFound() throws Exception {
    rinas
        .change(reset(UUID.randomUUID()), password(NEW_PASSWORD), "\"0\"")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void aDisabledAccountsOpenSessionsEndAtOnceAndItCannotLogIn() throws Exception {
    Browser bayus = logIn("bayu", BAYU_PASSWORD);

    rinas
        .change(disable(bayu), null, "\"0\"")
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"1\""))
        .andExpect(jsonPath("$.isDisabled").value(true))
        .andExpect(jsonPath("$.version").value(1));

    bayus
        .get(SessionController.PATH)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    logInFails("bayu", BAYU_PASSWORD, "ACCOUNT_DISABLED");
    // Only the right password learns that the account is disabled.
    logInFails("bayu", "a wrong password", "BAD_CREDENTIALS");
    rinas.get(USERS + "/" + bayu).andExpect(jsonPath("$.isDisabled").value(true));
  }

  @Test
  void anEnabledAccountLogsInAgain() throws Exception {
    rinas.change(disable(bayu), null, "\"0\"").andExpect(status().isOk());

    rinas
        .change(enable(bayu), null, "\"1\"")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isDisabled").value(false))
        .andExpect(jsonPath("$.version").value(2));
    logIn("bayu", BAYU_PASSWORD);
  }

  @Test
  void disablingADisabledAccountChangesNothing() throws Exception {
    rinas.change(disable(bayu), null, "\"0\"").andExpect(status().isOk());

    rinas
        .change(disable(bayu), null, "\"1\"")
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"1\""))
        .andExpect(jsonPath("$.isDisabled").value(true));
  }

  @Test
  void nobodyDisablesTheirOwnAccount() throws Exception {
    rinas
        .change(disable(rina), null, "\"0\"")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("CANNOT_DISABLE_OWN_ACCOUNT"));
    rinas.get(SessionController.PATH).andExpect(status().isOk());
  }

  @Test
  void aUserChangesTheirOwnPasswordAndStaysLoggedIn() throws Exception {
    rinas
        .change(changePassword(rina), passwordChange(PASSWORD, NEW_PASSWORD), "\"0\"")
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"1\""))
        .andExpect(jsonPath("$.version").value(1));

    rinas.get(SessionController.PATH).andExpect(status().isOk());
    logInFails("rina", PASSWORD, "BAD_CREDENTIALS");
    logIn("rina", NEW_PASSWORD);
  }

  @Test
  void aWrongCurrentPasswordAndAShortNewOneAreFieldErrorsAtOnce() throws Exception {
    rinas
        .change(changePassword(rina), passwordChange("not my password", "short"), "\"0\"")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].pointer").value("/currentPassword"))
        .andExpect(jsonPath("$.errors[0].code").value("WRONG_PASSWORD"))
        .andExpect(jsonPath("$.errors[1].pointer").value("/newPassword"))
        .andExpect(jsonPath("$.errors[1].code").value("TOO_SHORT"));
    logIn("rina", PASSWORD);
  }

  @Test
  void blankPasswordsAreABadRequest() throws Exception {
    rinas
        .change(changePassword(rina), passwordChange("", " "), "\"0\"")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].pointer").value("/currentPassword"))
        .andExpect(jsonPath("$.errors[1].pointer").value("/newPassword"));
  }

  @Test
  void nobodyChangesAnotherUsersPasswordThisWay() throws Exception {
    rinas
        .change(changePassword(bayu), passwordChange(BAYU_PASSWORD, NEW_PASSWORD), "\"0\"")
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("CAN_ONLY_CHANGE_OWN_PASSWORD"));
    logIn("bayu", BAYU_PASSWORD);
  }

  @Test
  void changingYourOwnPasswordNamesTheVersionToo() throws Exception {
    rinas
        .change(changePassword(rina), passwordChange(PASSWORD, NEW_PASSWORD), "\"0\"")
        .andExpect(status().isOk());

    rinas
        .change(changePassword(rina), passwordChange(NEW_PASSWORD, "yet another password"), "\"0\"")
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.changedBy.fullName").value("Rina Wulandari"));
  }

  @Test
  void wrongCurrentPasswordsAreSlowedDownLikeFailedLogins() throws Exception {
    // A login name of its own: the throttle lives as long as the backend, across tests.
    UUID dewi = users.create("dewi", "Dewi Lestari", PASSWORD);
    Browser dewis = logIn("dewi", PASSWORD);
    for (int i = 0; i < LoginThrottle.FREE_FAILURES; i++) {
      dewis
          .change(changePassword(dewi), passwordChange("a wrong guess", NEW_PASSWORD), "\"0\"")
          .andExpect(status().isUnprocessableContent());
    }

    dewis
        .change(changePassword(dewi), passwordChange(PASSWORD, NEW_PASSWORD), "\"0\"")
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.code").value("TOO_MANY_LOGIN_ATTEMPTS"));
  }

  @Test
  void anAccountIsNeverDeleted() throws Exception {
    rinas
        .delete(USERS + "/" + bayu)
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
  }

  private Browser logIn(String loginName, String password) throws Exception {
    Browser browser = new Browser(mvc).visit();
    browser.post(SessionController.PATH, login(loginName, password)).andExpect(status().isOk());
    return browser;
  }

  private void logInFails(String loginName, String password, String code) throws Exception {
    new Browser(mvc)
        .visit()
        .post(SessionController.PATH, login(loginName, password))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(code));
  }

  private static String reset(UUID id) {
    return USERS + "/" + id + "/reset-password";
  }

  private static String disable(UUID id) {
    return USERS + "/" + id + "/disable";
  }

  private static String enable(UUID id) {
    return USERS + "/" + id + "/enable";
  }

  private static String changePassword(UUID id) {
    return USERS + "/" + id + "/change-password";
  }

  private static String passwordChange(String currentPassword, String newPassword) {
    return """
    {"currentPassword": "%s", "newPassword": "%s"}\
    """
        .formatted(currentPassword, newPassword);
  }

  private static String password(String password) {
    return """
    {"password": "%s"}\
    """
        .formatted(password);
  }

  private static String login(String loginName, String password) {
    return """
    {"loginName": "%s", "password": "%s"}\
    """
        .formatted(loginName, password);
  }
}
