package com.niskora;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** A real PostgreSQL 18 in Testcontainers, wired into the backend as its datasource. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

  // The same image, pinned to the same digest, as compose.yaml (ENG-53). Testcontainers does not
  // recognise a digest reference as "postgres" by itself, so the substitute is declared.
  private static final DockerImageName POSTGRES_18 =
      DockerImageName.parse(
              "postgres:18@sha256:2ea8630df176ad090ec92136dee850397c5af8781e4bcf2e4162593691c61bbc")
          .asCompatibleSubstituteFor("postgres");

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgres() {
    return new PostgreSQLContainer(POSTGRES_18);
  }
}
