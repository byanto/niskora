package com.niskora.common.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Every error the HTTP layer answers is Problem Details with a stable code (ENG-65). */
@WebMvcTest(ProblemErrorController.class)
// Spring MVC's error handling alone: the security in front of it is tested in identity.
@AutoConfigureMockMvc(addFilters = false)
// Spring Boot leaves a test's nested classes out of scanning, so the controller is imported.
@Import(ProblemDetailsTest.FailingController.class)
class ProblemDetailsTest {

  @Autowired MockMvc mvc;

  @Test
  void anUnhandledErrorIsAnInternalServerErrorThatHidesItsMessage() throws Exception {
    mvc.perform(get("/test/failing"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
        .andExpect(content().string(not(containsString(SECRET))));
  }

  @Test
  void anUnknownPathIsNotFound() throws Exception {
    mvc.perform(get("/api/nothing-here"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.detail").value("Nothing is found at this path."));
  }

  @Test
  void aWrongMethodIsMethodNotAllowed() throws Exception {
    mvc.perform(post("/test/failing"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void anUnreadableBodyIsABadRequest() throws Exception {
    mvc.perform(post("/test/echo").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void anErrorRaisedOutsideSpringMvcIsAnsweredAsProblemDetailsByTheErrorPage() throws Exception {
    // The servlet container forwards an error raised outside Spring MVC, in a filter for example,
    // to /error with these request attributes.
    mvc.perform(
            get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 503)
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException(SECRET)))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(503))
        .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
        .andExpect(content().string(not(containsString(SECRET))));
  }

  @Test
  void theErrorPageAnswersAnInternalServerErrorWhenNoStatusIsGiven() throws Exception {
    mvc.perform(get("/error"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
  }

  private static final String SECRET = "connection to db-secret-host refused";

  @RestController
  static class FailingController {

    @GetMapping("/test/failing")
    String failing() {
      throw new IllegalStateException(SECRET);
    }

    @PostMapping("/test/echo")
    Message echo(@RequestBody Message message) {
      return message;
    }

    record Message(String text) {}
  }
}
