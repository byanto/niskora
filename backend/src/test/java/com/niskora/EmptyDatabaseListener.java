package com.niskora;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

/**
 * Before each test method, drops every schema in the database and runs every migration again
 * (ENG-35). It drops the schemas itself rather than through Flyway's clean, which only reaches the
 * schemas Flyway is told about, so a module's new schema (ENG-19) is covered without being listed
 * anywhere. It runs before a test's transaction begins.
 */
final class EmptyDatabaseListener implements TestExecutionListener, Ordered {

  @Override
  public void beforeTestMethod(TestContext testContext) {
    ApplicationContext context = testContext.getApplicationContext();
    JdbcClient jdbc = JdbcClient.create(context.getBean(DataSource.class));
    jdbc.sql(
            """
            SELECT nspname FROM pg_namespace
            WHERE nspname <> 'information_schema' AND NOT starts_with(nspname, 'pg_')
            """)
        .query(String.class)
        .list()
        .forEach(schema -> jdbc.sql("DROP SCHEMA \"" + schema + "\" CASCADE").update());
    jdbc.sql("CREATE SCHEMA public").update();
    context.getBean(Flyway.class).migrate();
  }

  @Override
  public int getOrder() {
    return new TransactionalTestExecutionListener().getOrder() - 1;
  }
}
