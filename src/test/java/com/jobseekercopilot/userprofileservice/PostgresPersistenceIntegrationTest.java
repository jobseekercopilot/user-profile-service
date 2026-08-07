package com.jobseekercopilot.userprofileservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class PostgresPersistenceIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("user_profile")
                    .withUsername("user_profile")
                    .withPassword("test-only-database-password");

    @BeforeEach
    void resetDatabase() {
        flyway().clean();
    }

    @Test
    void migratesEmptyPostgresAndEnforcesOwnershipAndDomainConstraints() throws SQLException {
        assertEquals(5, flyway().migrate().migrationsExecuted);

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            long profileId = insertProfile(statement, "profile-owner");
            statement.executeUpdate("""
                    INSERT INTO user_profile_skills (user_profile_id, skill)
                    VALUES (%d, 'Java')
                    """.formatted(profileId));

            SQLException duplicateOwner = assertThrows(SQLException.class,
                    () -> insertProfile(statement, "profile-owner"));
            assertEquals("23505", duplicateOwner.getSQLState());

            SQLException invalidRange = assertThrows(SQLException.class,
                    () -> statement.executeUpdate("""
                            INSERT INTO user_profile (user_id, commute_range)
                            VALUES ('invalid-range', 501)
                            """));
            assertEquals("23514", invalidRange.getSQLState());

            SQLException orphan = assertThrows(SQLException.class,
                    () -> statement.executeUpdate("""
                            INSERT INTO qualification (
                                user_profile_id, qualification_name, issuing_body, status
                            ) VALUES (999999, 'Qualification', 'Issuer', 'COMPLETED')
                            """));
            assertEquals("23503", orphan.getSQLState());

            SQLException conflictingAvailability = assertThrows(SQLException.class,
                    () -> statement.executeUpdate("""
                            INSERT INTO user_profile (
                                user_id, available_from, notice_period_days
                            ) VALUES ('invalid-availability', DATE '2026-08-01', 30)
                            """));
            assertEquals("23514", conflictingAvailability.getSQLState());

            statement.executeUpdate("""
                    INSERT INTO user_profile_workplace_arrangements (
                        user_profile_id, workplace_arrangement
                    ) VALUES (%d, 'REMOTE')
                    """.formatted(profileId));

            statement.executeUpdate("DELETE FROM user_profile WHERE id = " + profileId);
            assertEquals(0, count(statement, "user_profile_skills"));
        }
    }

    @Test
    void upgradesPreviousSchemaWithoutLosingNestedProfileData() throws SQLException {
        Flyway versionOne = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .cleanDisabled(false)
                .target("1")
                .load();
        assertEquals(1, versionOne.migrate().migrationsExecuted);

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            long profileId = insertProfile(statement, "retained-owner");
            statement.executeUpdate("""
                    INSERT INTO user_profile_skills (user_profile_id, skill)
                    VALUES (%d, 'Data analysis')
                    """.formatted(profileId));
            statement.executeUpdate("""
                    INSERT INTO user_profile_target_roles (user_profile_id, target_role)
                    VALUES (%d, 'Analyst')
                    """.formatted(profileId));
            statement.executeUpdate("""
                    INSERT INTO qualification (
                        user_profile_id, qualification_name, issuing_body, status, date_achieved
                    ) VALUES (%d, 'Certificate', 'Issuer', 'COMPLETED', '2026-07')
                    """.formatted(profileId));
            statement.executeUpdate("""
                    INSERT INTO role (
                        user_profile_id, job_title, employer, status, start_date
                    ) VALUES (%d, 'Engineer', 'Employer', 'CURRENT', '2026-01')
                    """.formatted(profileId));
        }

        assertEquals(4, flyway().migrate().migrationsExecuted);
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertEquals(1, count(statement, "user_profile"));
            assertEquals(1, count(statement, "user_profile_skills"));
            assertEquals(1, count(statement, "user_profile_target_roles"));
            assertEquals(1, count(statement, "qualification"));
            assertEquals(1, count(statement, "role"));
            assertEquals(8, indexCount(statement));
            assertEquals(1, profileRevision(statement, "retained-owner"));
            assertEquals(0, count(statement, "evidence_entry"));
            assertEquals(0, count(statement, "evidence_snapshot"));
            assertEquals(0, count(statement, "user_profile_employment_types"));
            assertEquals(0, count(statement, "user_profile_working_patterns"));
            assertEquals(0, count(statement, "user_profile_workplace_arrangements"));
        }
    }

    @Test
    void postgresKeepsLegacyNarrativesWhileBoundingGeneratedAndSnapshotFacts() throws SQLException {
        flyway().migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertEquals(4000, columnLength(statement, "evidence_revision", "description"));
            assertEquals(4000, columnLength(statement, "evidence_revision", "responsibilities"));
            assertEquals(4000, columnLength(statement, "evidence_revision", "achievements"));
            assertEquals(2000, columnLength(statement, "evidence_fact", "fact_value"));
            assertEquals(2000, columnLength(statement, "evidence_snapshot_fact", "fact_value"));
        }
    }

    @Test
    void postgresBackupRestoresProfileAndNestedRowsIntoSeparateDatabase() throws Exception {
        flyway().migrate();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            long profileId = insertProfile(statement, "backup-owner");
            statement.executeUpdate("""
                    INSERT INTO user_profile_skills (user_profile_id, skill)
                    VALUES (%d, 'Restore testing')
                    """.formatted(profileId));
            statement.executeUpdate("""
                    INSERT INTO role (
                        user_profile_id, job_title, employer, status, start_date
                    ) VALUES (%d, 'Tester', 'Employer', 'PREVIOUS_ROLE', '2025-01')
                    """.formatted(profileId));
        }

        assertSuccessful(POSTGRES.execInContainer(
                "pg_dump", "--username", POSTGRES.getUsername(), "--dbname", POSTGRES.getDatabaseName(),
                "--format=custom", "--file=/tmp/user-profile.dump"));
        assertSuccessful(POSTGRES.execInContainer(
                "createdb", "--username", POSTGRES.getUsername(), "user_profile_restore"));
        assertSuccessful(POSTGRES.execInContainer(
                "pg_restore", "--username", POSTGRES.getUsername(), "--dbname", "user_profile_restore",
                "--exit-on-error", "/tmp/user-profile.dump"));
        org.testcontainers.containers.Container.ExecResult counts = POSTGRES.execInContainer(
                "psql", "--username", POSTGRES.getUsername(), "--dbname", "user_profile_restore",
                "--tuples-only", "--no-align", "--command",
                "SELECT (SELECT COUNT(*) FROM user_profile) || ',' || "
                        + "(SELECT COUNT(*) FROM user_profile_skills) || ',' || "
                        + "(SELECT COUNT(*) FROM role);");

        assertSuccessful(counts);
        assertEquals("1,1,1", counts.getStdout().trim());
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .cleanDisabled(false)
                .load();
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private long insertProfile(Statement statement, String userId) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
                INSERT INTO user_profile (
                    user_id, target_weekly_hours, commute_range, postcode,
                    region, admin_district, latitude, longitude
                ) VALUES (
                    '%s', 'FULL_TIME', 25, 'SW1A 1AA',
                    'London', 'Westminster', 51.501, -0.142
                ) RETURNING id
                """.formatted(userId))) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private int count(Statement statement, String table) throws SQLException {
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private int indexCount(Statement statement) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname IN (
                    'idx_profile_skills_profile_id',
                    'idx_profile_target_roles_profile_id',
                    'idx_qualification_profile_id',
                    'idx_role_profile_id',
                    'idx_evidence_entry_owner',
                    'idx_evidence_entry_owner_state',
                    'idx_evidence_revision_entry',
                    'idx_evidence_fact_revision'
                  )
                """)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private int columnLength(Statement statement, String table, String column) throws SQLException {
        try (ResultSet result = statement.executeQuery("""
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = '%s'
                  AND column_name = '%s'
                """.formatted(table, column))) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private long profileRevision(Statement statement, String userId) throws SQLException {
        try (ResultSet result = statement.executeQuery(
                "SELECT profile_revision FROM user_profile WHERE user_id = '" + userId + "'")) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private void assertSuccessful(org.testcontainers.containers.Container.ExecResult result) {
        assertEquals(0, result.getExitCode(), result.getStderr());
    }
}
