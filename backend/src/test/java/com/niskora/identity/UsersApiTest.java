package com.niskora.identity;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.niskora.DatabaseTest;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The Users screen's accounts over HTTP (PLT-21, FE-37): every account as a paged list (ENG-76 to
 * ENG-79), and adding one with its field errors (ENG-81, ENG-82). Each test drives the API as the
 * browser does, logged in as rina.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DatabaseTest
class UsersApiTest {

  private static final String USERS = UsersController.PATH;
  private static final String PASSWORD = "correct horse battery";

  /** Any Argon2 hash: the accounts that only fill a list never log in. */
  private static final String SOME_HASH = "{argon2}$argon2id$v=19$m=16384,t=2,p=1$c2FsdA$aGFzaA";

  @Autowired MockMvc mvc;

  @Autowired Users users;

  @Autowired UserRepository repository;

  private Browser rina;

  @BeforeEach
  void logInAsRina() throws Exception {
    users.create("rina", "Rina Wulandari", PASSWORD);
    rina = new Browser(mvc).visit();
    rina.post(SessionController.PATH, login("rina", PASSWORD)).andExpect(status().isOk());
  }

  @Test
  void listsEveryAccountByFullNameInThePagedListShape() throws Exception {
    account("bayu", "bayu Santoso", "2026-11-17T02:00:00Z");
    account("sari", "Sari Dewi", "2026-11-18T02:00:00Z");

    rina.get(USERS)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        // By full name whatever its case.
        .andExpect(jsonPath("$.data[*].loginName").value(contains("bayu", "rina", "sari")))
        .andExpect(jsonPath("$.data[2].fullName").value("Sari Dewi"))
        .andExpect(jsonPath("$.data[2].createdAt").value("2026-11-18T02:00:00Z"))
        .andExpect(jsonPath("$.data[2].id").isString())
        .andExpect(jsonPath("$.data[2].passwordHash").doesNotExist())
        .andExpect(jsonPath("$.pagination.page").value(1))
        .andExpect(jsonPath("$.pagination.pageSize").value(50))
        .andExpect(jsonPath("$.pagination.totalItems").value(3))
        .andExpect(jsonPath("$.pagination.totalPages").value(1));
  }

  @Test
  void aPageHoldsFiftyAccountsAndPagesStartAtOne() throws Exception {
    IntStream.rangeClosed(1, 50)
        .forEach(n -> account("user%02d".formatted(n), "User %02d".formatted(n), null));

    rina.get(USERS + "?page=2")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(1)))
        // "User 50" comes after "Rina Wulandari" and the 49 users before it.
        .andExpect(jsonPath("$.data[0].loginName").value("user50"))
        .andExpect(jsonPath("$.pagination.page").value(2))
        .andExpect(jsonPath("$.pagination.totalItems").value(51))
        .andExpect(jsonPath("$.pagination.totalPages").value(2));
  }

  @Test
  void aPagePastTheLastIsEmpty() throws Exception {
    rina.get(USERS + "?page=3")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(0)))
        .andExpect(jsonPath("$.pagination.page").value(3))
        .andExpect(jsonPath("$.pagination.totalPages").value(1));
  }

  @Test
  void sortsByTheFieldsAsked() throws Exception {
    account("bayu", "Bayu Santoso", "2026-11-18T02:00:00Z");
    account("andi", "Zaki Andi", "2026-11-17T02:00:00Z");

    rina.get(USERS + "?sort=loginName")
        .andExpect(jsonPath("$.data[*].loginName").value(contains("andi", "bayu", "rina")));
    rina.get(USERS + "?sort=-fullName")
        .andExpect(jsonPath("$.data[*].loginName").value(contains("andi", "rina", "bayu")));
    rina.get(USERS + "?sort=createdAt")
        .andExpect(jsonPath("$.data[*].loginName").value(contains("andi", "bayu", "rina")));
  }

  @Test
  void anUnknownSortFieldIsABadRequest() throws Exception {
    rina.get(USERS + "?sort=passwordHash")
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void aPageBelowOneIsABadRequest() throws Exception {
    rina.get(USERS + "?page=0")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    rina.get(USERS + "?page=first")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void theListNeedsASession() throws Exception {
    new Browser(mvc).get(USERS).andExpect(status().isUnauthorized());
  }

  @Test
  void addsAnAccountThatCanLogIn() throws Exception {
    String body =
        rina.post(USERS, newUser(" sari ", " Sari Dewi ", "another password"), UUID.randomUUID())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.loginName").value("sari"))
            .andExpect(jsonPath("$.fullName").value("Sari Dewi"))
            .andExpect(jsonPath("$.createdAt").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = JsonPath.read(body, "$.id");

    rina.get(USERS).andExpect(jsonPath("$.data[*].loginName").value(contains("rina", "sari")));
    rina.get(USERS + "/" + id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loginName").value("sari"));
    new Browser(mvc)
        .visit()
        .post(SessionController.PATH, login("sari", "another password"))
        .andExpect(status().isOk());
  }

  @Test
  void anAddedAccountIsAtItsLocation() throws Exception {
    String location =
        rina.post(USERS, newUser("sari", "Sari Dewi", "another password"), UUID.randomUUID())
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("/api/identity/users/")))
            .andReturn()
            .getResponse()
            .getHeader("Location");

    rina.get(String.valueOf(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Sari Dewi"));
  }

  @Test
  void anUnknownAccountIsNotFound() throws Exception {
    rina.get(USERS + "/" + UUID.randomUUID())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void blankFieldsAreABadRequestWithEveryFieldError() throws Exception {
    rina.post(USERS, newUser("", " ", ""), UUID.randomUUID())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors", hasSize(3)))
        .andExpect(
            jsonPath("$.errors[*].pointer")
                .value(containsInAnyOrder("/loginName", "/fullName", "/password")))
        .andExpect(jsonPath("$.errors[*].code").value(contains("REQUIRED", "REQUIRED", "REQUIRED")))
        .andExpect(jsonPath("$.errors[0].detail").isString());
  }

  @Test
  void aMissingFieldIsRequiredToo() throws Exception {
    rina.post(
            USERS,
            "{\"loginName\": \"sari\", \"password\": \"another password\"}",
            UUID.randomUUID())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].pointer").value("/fullName"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
  }

  @Test
  void aTakenLoginNameIsAFieldError() throws Exception {
    rina.post(USERS, newUser("RINA", "Rina Kusuma", "another password"), UUID.randomUUID())
        .andExpect(status().isUnprocessableContent())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors", hasSize(1)))
        .andExpect(jsonPath("$.errors[0].pointer").value("/loginName"))
        .andExpect(jsonPath("$.errors[0].code").value("LOGIN_NAME_TAKEN"));
  }

  @Test
  void everyBrokenRuleIsAFieldErrorAtOnce() throws Exception {
    rina.post(USERS, newUser("sari dewi", "Sari Dewi", "short"), UUID.randomUUID())
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors", hasSize(2)))
        .andExpect(jsonPath("$.errors[0].pointer").value("/loginName"))
        .andExpect(jsonPath("$.errors[0].code").value("MUST_BE_ONE_WORD"))
        .andExpect(jsonPath("$.errors[1].pointer").value("/password"))
        .andExpect(jsonPath("$.errors[1].code").value("TOO_SHORT"));
  }

  /** An account straight into the table, for a list to show: hashing a password takes a while. */
  private void account(String loginName, String fullName, @Nullable String createdAt) {
    repository.saveAndFlush(
        new User(
            UUID.randomUUID(),
            loginName,
            fullName,
            SOME_HASH,
            createdAt == null ? Instant.parse("2026-11-16T02:00:00Z") : Instant.parse(createdAt)));
  }

  private static String newUser(String loginName, String fullName, String password) {
    return """
    {"loginName": "%s", "fullName": "%s", "password": "%s"}\
    """
        .formatted(loginName, fullName, password);
  }

  private static String login(String loginName, String password) {
    return """
    {"loginName": "%s", "password": "%s"}\
    """
        .formatted(loginName, password);
  }
}
