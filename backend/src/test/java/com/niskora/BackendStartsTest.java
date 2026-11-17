package com.niskora;

import static org.assertj.core.api.Assertions.assertThat;

import com.niskora.common.BusinessClock;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Starts the whole backend against a real PostgreSQL 18. Startup fails if Flyway cannot migrate or
 * Hibernate's schema validation fails, so a passing health check covers both.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DatabaseTest
@ExtendWith(OutputCaptureExtension.class)
class BackendStartsTest {

  @LocalServerPort int applicationPort;

  @LocalManagementPort int managementPort;

  @Autowired Clock clock;

  @Autowired BusinessClock businessClock;

  @Test
  void answersABareUpOnTheInternalManagementPort() {
    clientOn(managementPort)
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json("{\"status\":\"UP\"}", JsonCompareMode.STRICT);
  }

  @Test
  void exposesNoActuatorEndpointButHealth() {
    clientOn(managementPort).get().uri("/actuator/env").exchange().expectStatus().isNotFound();
    clientOn(managementPort).get().uri("/actuator/beans").exchange().expectStatus().isNotFound();
  }

  @Test
  void servesNoActuatorEndpointOnTheApplicationPort() {
    clientOn(applicationPort).get().uri("/actuator/health").exchange().expectStatus().isNotFound();
  }

  @Test
  void answersAnUnknownPathWithProblemDetails() {
    clientOn(applicationPort)
        .get()
        .uri("/api/health")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(404)
        .jsonPath("$.code")
        .isEqualTo("NOT_FOUND");
  }

  @Test
  void logsOneJsonObjectPerLineToStdout(CapturedOutput output) {
    LoggerFactory.getLogger(BackendStartsTest.class).info("structured log probe");

    String line =
        output
            .getOut()
            .lines()
            .filter(candidate -> candidate.contains("structured log probe"))
            .findFirst()
            .orElseThrow();
    JsonNode entry = JsonMapper.shared().readTree(line);
    assertThat(entry.path("message").asString()).isEqualTo("structured log probe");
    assertThat(entry.path("log").path("level").asString()).isEqualTo("INFO");
    assertThat(entry.path("@timestamp").asString()).endsWith("Z");
  }

  @Test
  void runsOnAUtcClockAndOffersTheBusinessClock() {
    assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    assertThat(Duration.between(businessClock.now(), Instant.now()).abs())
        .isLessThan(Duration.ofMinutes(1));
  }

  private static RestTestClient clientOn(int port) {
    return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }
}
