package com.niskora.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.niskora.DatabaseTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The session and CSRF token cookies as the servlet container writes them (PLT-17). Only a real
 * server sets their attributes, so this runs on one. The annotations match {@code
 * BackendStartsTest}, so both share one Spring context and one container.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DatabaseTest
class SessionCookieTest {

  @LocalServerPort int port;

  @Autowired Users users;

  @Test
  void theSessionCookieIsSecureHttpOnlyAndLaxAndTheCsrfCookieIsReadable() {
    users.create("rina", "Rina Wulandari", "correct horse battery");
    RestTestClient client =
        RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

    String csrfCookie =
        cookie(
            client.get().uri(SecurityConfiguration.SESSION_PATH).exchange().returnResult(),
            "XSRF-TOKEN");
    assertThat(csrfCookie).contains("Secure").contains("SameSite=Lax").doesNotContain("HttpOnly");
    String token = csrfCookie.substring("XSRF-TOKEN=".length(), csrfCookie.indexOf(';'));

    var login =
        client
            .post()
            .uri(SecurityConfiguration.SESSION_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .cookie("XSRF-TOKEN", token)
            .header("X-XSRF-TOKEN", token)
            .body(
                """
                {"loginName": "rina", "password": "correct horse battery"}\
                """)
            .exchange()
            .returnResult();

    assertThat(login.getStatus().value()).isEqualTo(200);
    assertThat(cookie(login, "JSESSIONID"))
        .contains("Secure")
        .contains("HttpOnly")
        .contains("SameSite=Lax");
  }

  private static String cookie(
      org.springframework.test.web.servlet.client.ExchangeResult result, String name) {
    List<String> cookies = result.getResponseHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
    return cookies.stream()
        .filter(cookie -> cookie.startsWith(name + "=") && !cookie.startsWith(name + "=;"))
        .reduce((first, last) -> last)
        .orElseThrow(() -> new AssertionError("No " + name + " cookie in " + cookies));
  }
}
