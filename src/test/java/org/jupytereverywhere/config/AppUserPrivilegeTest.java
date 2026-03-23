package org.jupytereverywhere.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test verifying that the V8 migration creates app_user with correct DML-only
 * privileges.
 */
@Testcontainers
class AppUserPrivilegeTest {

  @SuppressWarnings("resource")
  @Container
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15")
          .withDatabaseName("testdb")
          .withUsername("admin")
          .withPassword("admin");

  private static Connection adminConn;
  private static Connection appUserConn;

  @BeforeAll
  static void setUp() throws Exception {
    adminConn =
        DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

    // Create a sample table to test privileges against
    try (Statement stmt = adminConn.createStatement()) {
      stmt.execute("CREATE TABLE test_notebooks (id SERIAL PRIMARY KEY, name TEXT NOT NULL)");
      stmt.execute("INSERT INTO test_notebooks (name) VALUES ('sample')");
    }

    // Run the V8 migration SQL manually (simulating Flyway)
    try (Statement stmt = adminConn.createStatement()) {
      // 1. Idempotent role creation
      stmt.execute(
          "DO $$ BEGIN "
              + "IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_user') THEN "
              + "CREATE ROLE app_user WITH LOGIN; "
              + "END IF; "
              + "END $$");

      // 2. Grant rds_iam — skip since rds_iam doesn't exist on local Postgres
      // (the migration handles this via exception handler)

      // 3. Grants on existing objects
      stmt.execute("GRANT USAGE ON SCHEMA public TO app_user");
      stmt.execute(
          "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user");
      stmt.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user");

      // 4. Default privileges for future objects
      stmt.execute(
          "ALTER DEFAULT PRIVILEGES IN SCHEMA public "
              + "GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user");
      stmt.execute(
          "ALTER DEFAULT PRIVILEGES IN SCHEMA public "
              + "GRANT USAGE, SELECT ON SEQUENCES TO app_user");
    }

    // Connect as app_user (no password needed for local trust auth in Testcontainers)
    // Testcontainers uses trust auth by default, but we need to set a password for app_user
    try (Statement stmt = adminConn.createStatement()) {
      stmt.execute("ALTER ROLE app_user WITH PASSWORD 'app_user_pass'");
    }
    appUserConn = DriverManager.getConnection(postgres.getJdbcUrl(), "app_user", "app_user_pass");
  }

  @AfterAll
  static void tearDown() throws Exception {
    if (appUserConn != null) appUserConn.close();
    if (adminConn != null) adminConn.close();
  }

  @Test
  void appUserCanSelect() {
    assertDoesNotThrow(
        () -> {
          try (Statement stmt = appUserConn.createStatement()) {
            stmt.executeQuery("SELECT * FROM test_notebooks");
          }
        });
  }

  @Test
  void appUserCanInsert() {
    assertDoesNotThrow(
        () -> {
          try (Statement stmt = appUserConn.createStatement()) {
            stmt.execute("INSERT INTO test_notebooks (name) VALUES ('new')");
          }
        });
  }

  @Test
  void appUserCanUpdate() {
    assertDoesNotThrow(
        () -> {
          try (Statement stmt = appUserConn.createStatement()) {
            stmt.execute("UPDATE test_notebooks SET name = 'updated' WHERE name = 'sample'");
          }
        });
  }

  @Test
  void appUserCanDelete() {
    assertDoesNotThrow(
        () -> {
          try (Statement stmt = appUserConn.createStatement()) {
            stmt.execute("DELETE FROM test_notebooks WHERE name = 'new'");
          }
        });
  }

  @Test
  void appUserCannotCreateTable() {
    SQLException ex =
        assertThrows(
            SQLException.class,
            () -> {
              try (Statement stmt = appUserConn.createStatement()) {
                stmt.execute("CREATE TABLE unauthorized_table (id SERIAL PRIMARY KEY)");
              }
            });
    assertTrue(ex.getMessage().contains("permission denied"));
  }

  @Test
  void appUserCannotDropTable() {
    SQLException ex =
        assertThrows(
            SQLException.class,
            () -> {
              try (Statement stmt = appUserConn.createStatement()) {
                stmt.execute("DROP TABLE test_notebooks");
              }
            });
    assertTrue(ex.getMessage().contains("must be owner"));
  }

  @Test
  void appUserCannotAlterTable() {
    SQLException ex =
        assertThrows(
            SQLException.class,
            () -> {
              try (Statement stmt = appUserConn.createStatement()) {
                stmt.execute("ALTER TABLE test_notebooks ADD COLUMN extra TEXT");
              }
            });
    assertTrue(ex.getMessage().contains("must be owner"));
  }

  @Test
  void migrationIsIdempotent() {
    // Running the role creation block a second time should not error
    assertDoesNotThrow(
        () -> {
          try (Statement stmt = adminConn.createStatement()) {
            stmt.execute(
                "DO $$ BEGIN "
                    + "IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'app_user') THEN "
                    + "CREATE ROLE app_user WITH LOGIN; "
                    + "END IF; "
                    + "END $$");
            stmt.execute("GRANT USAGE ON SCHEMA public TO app_user");
            stmt.execute(
                "GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user");
            stmt.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user");
          }
        });
  }
}
