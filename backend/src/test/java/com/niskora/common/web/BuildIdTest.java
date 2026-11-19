package com.niskora.common.web;

import com.niskora.DatabaseTest;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Every response carries the backend's build id, so the frontend can tell when a new version is
 * running (ENG-92). It runs on the real servlet container, because only that forwards an error
 * raised outside Spring MVC to the error page.
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = "niskora.build-id=" + BuildIdTest.BUILD_ID)
@DatabaseTest
@Import(BuildIdTest.FailingFilterConfiguration.class)
class BuildIdTest {

  static final String BUILD_ID = "4f2c9e1";

  private static final String FAILING_PATH = "/test/failing-filter";

  @LocalServerPort int port;

  @LocalManagementPort int managementPort;

  @Test
  void aResponseFromSpringMvcCarriesTheBuildId() {
    // Without a session the call is refused, and the refusal is answered by Spring MVC's error
    // handling like any other error.
    client()
        .get()
        .uri("/api/nothing-here")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueEquals(BuildIdFilter.HEADER, BUILD_ID);
  }

  @Test
  void anErrorAnsweredByTheErrorPageCarriesTheBuildId() {
    client()
        .get()
        .uri(FAILING_PATH)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        .expectHeader()
        .valueEquals(BuildIdFilter.HEADER, BUILD_ID);
  }

  /** Caddy forwards the public /api/health to this endpoint on Actuator's own port (ENG-68). */
  @Test
  void theHealthCheckOnTheManagementPortCarriesTheBuildId() {
    clientOn(managementPort)
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals(BuildIdFilter.HEADER, BUILD_ID);
  }

  private RestTestClient client() {
    return clientOn(port);
  }

  private static RestTestClient clientOn(int port) {
    return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FailingFilterConfiguration {

    /** Fails outside Spring MVC, so the servlet container forwards the request to /error. */
    @Bean
    FilterRegistrationBean<Filter> failingFilter() {
      FilterRegistrationBean<Filter> registration =
          new FilterRegistrationBean<>((request, response, chain) -> {
            throw new IllegalStateException("a filter failed");
          });
      registration.addUrlPatterns(FAILING_PATH);
      // Before Spring Security, which would otherwise refuse the call first.
      registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
      return registration;
    }
  }
}
