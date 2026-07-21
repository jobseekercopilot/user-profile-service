package com.jobseekercopilot.userprofileservice.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductionDatabaseSafetyConfigurationTest {

    @Test
    void acceptsPostgresWithExplicitCredentials() {
        var configuration = new ProductionDatabaseSafetyConfiguration(
                "jdbc:postgresql://database:5432/user_profile", "user_profile", "secret");

        assertDoesNotThrow(configuration::validate);
    }

    @Test
    void rejectsNonPostgresDatabaseWithoutDisclosingItsUrl() {
        var configuration = new ProductionDatabaseSafetyConfiguration(
                "jdbc:h2:file:/sensitive/path", "user_profile", "secret");

        IllegalStateException failure = assertThrows(IllegalStateException.class, configuration::validate);

        assertEquals("Production profile database must use PostgreSQL.", failure.getMessage());
    }

    @Test
    void rejectsBlankCredentialsWithoutDisclosingThem() {
        var configuration = new ProductionDatabaseSafetyConfiguration(
                "jdbc:postgresql://database:5432/user_profile", "", "");

        IllegalStateException failure = assertThrows(IllegalStateException.class, configuration::validate);

        assertEquals("Production profile database credentials are required.", failure.getMessage());
    }
}
