package com.jobseekercopilot.userprofileservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "environment-data")
public class EnvironmentDataProperties {
    private boolean enabled = false;
    private String token = "";
    private List<String> allowedEnvironments = List.of("local", "test", "demo");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<String> getAllowedEnvironments() {
        return allowedEnvironments;
    }

    public void setAllowedEnvironments(List<String> allowedEnvironments) {
        this.allowedEnvironments = allowedEnvironments;
    }
}
