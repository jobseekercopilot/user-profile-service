package com.jobseekercopilot.userprofileservice.systemdata;

import com.jobseekercopilot.userprofileservice.config.EnvironmentDataProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile("environment-data")
public class EnvironmentDataGuard {
    public static final String PROFILE = "environment-data";
    public static final String TOKEN_HEADER = "X-Environment-Data-Token";
    private static final int MINIMUM_TOKEN_LENGTH = 32;
    private static final Set<String> SAFE_ENVIRONMENTS = Set.of("local", "test", "demo");
    private static final Set<String> PRODUCTION_LIKE_ENVIRONMENTS = Set.of(
            "prod", "production", "stage", "staging", "uat", "preprod", "live");

    private final EnvironmentDataProperties properties;
    private final Environment environment;

    public EnvironmentDataGuard(EnvironmentDataProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Environment data management must be explicitly enabled.");
        }
        if (properties.getToken() == null || properties.getToken().length() < MINIMUM_TOKEN_LENGTH) {
            throw new IllegalStateException("Environment data token must contain at least 32 characters.");
        }

        Set<String> configuredEnvironments = normalize(properties.getAllowedEnvironments());
        if (configuredEnvironments.isEmpty() || !SAFE_ENVIRONMENTS.containsAll(configuredEnvironments)) {
            throw new IllegalStateException("Environment data allowed environments must be a non-empty subset of local, test and demo.");
        }

        Set<String> activeProfiles = activeProfiles();
        if (!activeProfiles.contains(PROFILE)) {
            throw new IllegalStateException("The environment-data profile is required.");
        }
        if (activeProfiles.stream().anyMatch(PRODUCTION_LIKE_ENVIRONMENTS::contains)) {
            throw new IllegalStateException("Environment data management is forbidden in production-like environments.");
        }

        Set<String> runtimeEnvironments = activeProfiles.stream()
                .filter(profile -> !PROFILE.equals(profile))
                .collect(Collectors.toSet());
        if (runtimeEnvironments.size() != 1 || !configuredEnvironments.containsAll(runtimeEnvironments)) {
            throw new IllegalStateException("Exactly one approved environment profile must be active with environment-data.");
        }
    }

    public void requireEnabled() {
        try {
            validateConfiguration();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Environment data management is disabled");
        }
    }

    public boolean hasValidToken(String suppliedToken) {
        if (suppliedToken == null) {
            return false;
        }
        byte[] expected = properties.getToken().getBytes(StandardCharsets.UTF_8);
        byte[] supplied = suppliedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, supplied);
    }

    public String activeEnvironment() {
        String[] profiles = environment.getActiveProfiles();
        return Arrays.stream(profiles)
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .filter(profile -> !PROFILE.equals(profile))
                .sorted()
                .collect(Collectors.joining(","));
    }

    private Set<String> activeProfiles() {
        return normalize(Arrays.asList(environment.getActiveProfiles()));
    }

    private Set<String> normalize(Iterable<String> values) {
        java.util.HashSet<String> normalized = new java.util.HashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Set.copyOf(normalized);
    }
}
