package com.niskora;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Starts the whole backend against a real PostgreSQL 18. Startup fails if Flyway cannot migrate or
 * Hibernate's schema validation fails, so a passing health check covers both.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfiguration.class)
class BackendStartsTest {

  @LocalServerPort int applicationPort;

  @LocalManagementPort int managementPort;

  @Test
  void answersUpOnTheInternalManagementPort() {
    clientOn(managementPort)
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json("{\"status\":\"UP\"}");
  }

  @Test
  void servesNoActuatorEndpointOnTheApplicationPort() {
    clientOn(applicationPort).get().uri("/actuator/health").exchange().expectStatus().isNotFound();
    clientOn(applicationPort).get().uri("/api/health").exchange().expectStatus().isNotFound();
  }

  private static RestTestClient clientOn(int port) {
    return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }
}
