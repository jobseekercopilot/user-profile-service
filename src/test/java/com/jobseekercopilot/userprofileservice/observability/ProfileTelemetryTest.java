package com.jobseekercopilot.userprofileservice.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.jobseekercopilot.userprofileservice.observability.ProfileTelemetry.OperationType;
import com.jobseekercopilot.userprofileservice.observability.ProfileTelemetry.Outcome;
import com.jobseekercopilot.userprofileservice.observability.ProfileTelemetry.StatusFamily;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProfileTelemetryTest {

    @Test
    void recordsOnlyBoundedOutcomeAndLatencyTags() {
        var registry = new SimpleMeterRegistry();
        var telemetry = new ProfileTelemetry(registry);

        telemetry.record(OperationType.READ, Outcome.NOT_FOUND, StatusFamily.CLIENT_ERROR, 10_000_000);
        telemetry.record(OperationType.UPSERT, Outcome.CONFLICT, StatusFamily.CLIENT_ERROR, 20_000_000);

        assertEquals(1.0, registry.get("jobseeker.user.profile.operation.outcomes")
                .tags("operation", "read", "outcome", "not_found", "status", "4xx")
                .counter().count());
        assertEquals(1, registry.get("jobseeker.user.profile.operation.duration")
                .tags("operation", "upsert", "outcome", "conflict", "status", "4xx")
                .timer().count());

        Set<String> allowedTags = Set.of("operation", "outcome", "status", "le");
        for (Meter meter : registry.getMeters()) {
            meter.getId().getTags().forEach(tag -> {
                assertFalse(!allowedTags.contains(tag.getKey()), "unexpected tag: " + tag.getKey());
                assertFalse(tag.getValue().contains("user-123"));
                assertFalse(tag.getValue().contains("profile-456"));
                assertFalse(tag.getValue().contains("example.test"));
            });
        }
    }
}
