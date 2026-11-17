package com.niskora;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Every database test starts from an empty database with all migrations run (ENG-35). The first
 * test leaves data behind on purpose, in {@code public} and in a schema of its own as a module
 * would (ENG-19), and the second, run after it, must see none of it. The annotations match {@link
 * BackendStartsTest}, so both share one Spring context and one container.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DatabaseTest
@TestMethodOrder(OrderAnnotation.class)
class EmptyDatabaseTest {

  @Autowired JdbcClient jdbc;

  @Test
  @Order(1)
  void aTestMayLeaveDataBehind() {
    jdbc.sql("CREATE TABLE left_behind (id integer)").update();
    jdbc.sql("CREATE SCHEMA some_module").update();
    jdbc.sql("CREATE TABLE some_module.left_behind (id integer)").update();

    assertThat(exists("public.left_behind")).isTrue();
    assertThat(exists("some_module.left_behind")).isTrue();
  }

  @Test
  @Order(2)
  void theNextTestStartsFromAnEmptyDatabaseWithEveryMigrationRun() {
    assertThat(exists("public.left_behind")).isFalse();
    assertThat(schemaExists("some_module")).isFalse();
    assertThat(exists("public.flyway_schema_history")).isTrue();
  }

  private boolean exists(String table) {
    return jdbc.sql("SELECT to_regclass(:table) IS NOT NULL")
        .param("table", table)
        .query(Boolean.class)
        .single();
  }

  private boolean schemaExists(String schema) {
    return jdbc.sql("SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = :schema)")
        .param("schema", schema)
        .query(Boolean.class)
        .single();
  }
}
