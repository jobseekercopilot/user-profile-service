package com.jobseekercopilot.userprofileservice.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileSecurityConfigTest {

    @Test
    void acceptsAbsoluteHttpAndHttpsJwksUris() {
        assertDoesNotThrow(() -> ProfileSecurityConfig.validateConfiguration(
                "https://auth.example.test/.well-known/jwks.json", "issuer", "audience"));
        assertDoesNotThrow(() -> ProfileSecurityConfig.validateConfiguration(
                "http://authentication-service:8084/.well-known/jwks.json", "issuer", "audience"));
    }

    @Test
    void rejectsMissingUnsafeAndIncompleteConfigurationWithoutEchoingValues() {
        for (String uri : java.util.List.of(
                "relative/jwks.json", "file:///private/key", "ftp://auth.example.test/jwks",
                "https://user:password@auth.example.test/jwks", "https://auth.example.test/jwks#fragment")) {
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> ProfileSecurityConfig.validateConfiguration(uri, "issuer", "audience"));
            assertEquals("Profile JWT verification configuration is invalid", exception.getMessage());
            assertFalse(exception.getMessage().contains(uri));
        }
        assertThrows(IllegalStateException.class,
                () -> ProfileSecurityConfig.validateConfiguration(null, "issuer", "audience"));
        assertThrows(IllegalStateException.class,
                () -> ProfileSecurityConfig.validateConfiguration("https://auth.example.test/jwks", " ", "audience"));
        assertThrows(IllegalStateException.class,
                () -> ProfileSecurityConfig.validateConfiguration("https://auth.example.test/jwks", "issuer", " "));
    }
}
