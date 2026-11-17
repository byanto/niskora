package com.niskora;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;

/**
 * A test against a real PostgreSQL 18 in Testcontainers, for use case and adapter tests (ENG-33).
 * Put it beside {@code @SpringBootTest} or Modulith's {@code @ApplicationModuleTest}. Each test
 * method starts from an empty database with every migration run (ENG-35).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(PostgresTestConfiguration.class)
@TestExecutionListeners(
    listeners = EmptyDatabaseListener.class,
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public @interface DatabaseTest {}
