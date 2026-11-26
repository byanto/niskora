package com.niskora.identity;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Logging in, the session and its CSRF protection over HTTP (PLT-12, PLT-19, FE-71 to FE-74). Each
 * test drives the API as the browser does, through {@link Browser}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DatabaseTest
class SessionTest {

  private static final String SESSION = SessionController.PATH;
  private static final String PASSWORD = "correct horse battery";

  @Autowired MockMvc mvc;

  @Autowired Users users;

  private UUID rina;

  @BeforeEach
  void createUser() {
    rina = users.create("rina", "Rina Wulandari", PASSWORD);
  }

  @Test
  void aCallWithoutASessionIsUnauthorizedAndHandsOutTheCsrfToken() throws Exception {
    Browser browser = new Browser(mvc);

    browser
        .get(SESSION)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    assertThat(browser.csrfToken()).isNotBlank();
  }

  @Test
  void anyOtherPathWithoutASessionIsUnauthorizedToo() throws Exception {
    new Browser(mvc)
        .get("/api/nothing-here")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void loggingInStartsASessionThatSaysWhoIsLoggedIn() throws Exception {
    Browser browser = new Browser(mvc).visit();

    browser
        .post(SESSION, login("rina", PASSWORD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.id").value(rina.toString()))
        .andExpect(jsonPath("$.user.loginName").value("rina"))
        .andExpect(jsonPath("$.user.fullName").value("Rina Wulandari"));

    browser
        .get(SESSION)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.id").value(rina.toString()))
        .andExpect(jsonPath("$.user.fullName").value("Rina Wulandari"));
  }

  @Test
  void theLoginNameMatchesWhateverItsCase() throws Exception {
    new Browser(mvc)
        .visit()
        .post(SESSION, login("Rina", PASSWORD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.loginName").value("rina"));
  }

  @Test
  void aWrongPasswordIsRefusedAndStartsNoSession() throws Exception {
    Browser browser = new Browser(mvc).visit();

    browser
        .post(SESSION, login("rina", "wrong password"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));

    browser.get(SESSION).andExpect(status().isUnauthorized());
  }

  @Test
  void anUnknownLoginNameIsRefusedTheSameWay() throws Exception {
    new Browser(mvc)
        .visit()
        .post(SESSION, login("nobody", PASSWORD))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
  }

  @Test
  void aBlankLoginNameIsABadRequestWithItsFieldError() throws Exception {
    new Browser(mvc)
        .visit()
        .post(SESSION, login("", PASSWORD))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].pointer").value("/loginName"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
  }

  @Test
  void aChangeWithoutTheCsrfTokenIsForbidden() throws Exception {
    new Browser(mvc)
        .visit()
        .postWithoutCsrfToken(SESSION, login("rina", PASSWORD))
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
  }

  @Test
  void loggingInChangesTheSessionIdAndTheCsrfToken() throws Exception {
    Browser browser = new Browser(mvc).withSession(new MockHttpSession()).visit();
    String sessionBefore = browser.sessionId();
    String tokenBefore = browser.csrfToken();

    browser.post(SESSION, login("rina", PASSWORD)).andExpect(status().isOk());

    assertThat(browser.sessionId()).isNotEqualTo(sessionBefore);
    assertThat(browser.csrfToken()).isNotBlank().isNotEqualTo(tokenBefore);
    browser.get(SESSION).andExpect(status().isOk());
  }

  @Test
  void loggingOutEndsTheSession() throws Exception {
    Browser browser = new Browser(mvc).visit();
    browser.post(SESSION, login("rina", PASSWORD)).andExpect(status().isOk());

    browser.delete(SESSION).andExpect(status().isNoContent());

    browser.get(SESSION).andExpect(status().isUnauthorized());
  }

  @Test
  void loggingOutWithoutTheCsrfTokenIsForbidden() throws Exception {
    Browser browser = new Browser(mvc).visit();
    browser.post(SESSION, login("rina", PASSWORD)).andExpect(status().isOk());

    browser
        .deleteWithoutCsrfToken(SESSION)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    browser.get(SESSION).andExpect(status().isOk());
  }

  @Test
  void repeatedFailedLoginsAreSlowedDownEvenForTheRightPassword() throws Exception {
    // A login name of its own: the throttle lives as long as the backend, across tests.
    users.create("sari", "Sari Dewi", PASSWORD);
    Browser browser = new Browser(mvc).visit();
    for (int i = 0; i < LoginThrottle.FREE_FAILURES; i++) {
      browser.post(SESSION, login("sari", "wrong password")).andExpect(status().isUnauthorized());
    }

    browser
        .post(SESSION, login("sari", PASSWORD))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "1"))
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("TOO_MANY_LOGIN_ATTEMPTS"));
    browser.get(SESSION).andExpect(status().isUnauthorized());
  }

  private static String login(String loginName, String password) {
    return """
    {"loginName": "%s", "password": "%s"}\
    """
        .formatted(loginName, password);
  }
}
