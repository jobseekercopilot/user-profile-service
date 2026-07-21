package com.jobseekercopilot.userprofileservice.systemdata;

import com.jobseekercopilot.userprofileservice.config.EnvironmentDataProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentDataGuardTest {
    private static final String VALID_TOKEN = "profile-environment-data-token-123456789";

    @Test
    void acceptsOneExplicitApprovedEnvironment() {
        EnvironmentDataGuard guard = guard(List.of("local", "test", "demo"), "local", "environment-data");

        assertDoesNotThrow(guard::validateConfiguration);
        assertTrue(guard.hasValidToken(VALID_TOKEN));
        assertFalse(guard.hasValidToken("wrong-profile-environment-data-token"));
        assertFalse(guard.hasValidToken(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "production", "stage", "staging", "uat", "preprod", "live"})
    void rejectsEveryProductionLikeProfile(String productionProfile) {
        EnvironmentDataGuard guard = guard(List.of("local", "test", "demo"),
                productionProfile, "environment-data");

        assertThrows(IllegalStateException.class, guard::validateConfiguration);
    }

    @Test
    void rejectsAmbiguousMultipleEnvironmentProfiles() {
        EnvironmentDataGuard guard = guard(List.of("local", "test", "demo"),
                "local", "test", "environment-data");

        assertThrows(IllegalStateException.class, guard::validateConfiguration);
    }

    @Test
    void rejectsUnknownEnvironmentProfile() {
        EnvironmentDataGuard guard = guard(List.of("local", "test", "demo"),
                "developer-laptop", "environment-data");

        assertThrows(IllegalStateException.class, guard::validateConfiguration);
    }

    @Test
    void rejectsUnsafeConfiguredAllowList() {
        EnvironmentDataGuard guard = guard(List.of("local", "production"),
                "local", "environment-data");

        assertThrows(IllegalStateException.class, guard::validateConfiguration);
    }

    @Test
    void rejectsMissingExplicitProfile() {
        EnvironmentDataGuard guard = guard(List.of("local", "test", "demo"), "local");

        assertThrows(IllegalStateException.class, guard::validateConfiguration);
    }

    @Test
    void rejectsDisabledManagementAndWeakCredential() {
        EnvironmentDataProperties disabled = properties();
        disabled.setEnabled(false);
        EnvironmentDataGuard disabledGuard = new EnvironmentDataGuard(disabled,
                new MockEnvironment().withProperty("spring.profiles.active", "local,environment-data"));

        EnvironmentDataProperties weak = properties();
        weak.setToken("too-short");
        EnvironmentDataGuard weakGuard = new EnvironmentDataGuard(weak,
                new MockEnvironment().withProperty("spring.profiles.active", "local,environment-data"));

        assertThrows(IllegalStateException.class, disabledGuard::validateConfiguration);
        assertThrows(IllegalStateException.class, weakGuard::validateConfiguration);
    }

    private EnvironmentDataGuard guard(List<String> allowed, String... profiles) {
        EnvironmentDataProperties properties = properties();
        properties.setAllowedEnvironments(allowed);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return new EnvironmentDataGuard(properties, environment);
    }

    private EnvironmentDataProperties properties() {
        EnvironmentDataProperties properties = new EnvironmentDataProperties();
        properties.setEnabled(true);
        properties.setToken(VALID_TOKEN);
        return properties;
    }
}
