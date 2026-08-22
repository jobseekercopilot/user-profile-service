package com.jobseekercopilot.userprofileservice.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class ProfileTelemetry {

    public enum OperationType {
        READ("read"),
        UPSERT("upsert");

        private final String tag;

        OperationType(String tag) {
            this.tag = tag;
        }
    }

    public enum Outcome {
        SUCCESS("success"),
        NOT_FOUND("not_found"),
        INVALID_REQUEST("invalid_request"),
        CONFLICT("conflict"),
        INTERNAL_ERROR("internal_error");

        private final String tag;

        Outcome(String tag) {
            this.tag = tag;
        }
    }

    public enum StatusFamily {
        SUCCESS("2xx"),
        CLIENT_ERROR("4xx"),
        SERVER_ERROR("5xx");

        private final String tag;

        StatusFamily(String tag) {
            this.tag = tag;
        }
    }

    private final MeterRegistry registry;

    public ProfileTelemetry(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(OperationType operation, Outcome outcome, StatusFamily statusFamily, long elapsedNanos) {
        Counter.builder("jobseeker.user.profile.operation.outcomes")
                .description("Completed profile operations by bounded outcome")
                .tags("operation", operation.tag, "outcome", outcome.tag, "status", statusFamily.tag)
                .register(registry)
                .increment();

        Timer.builder("jobseeker.user.profile.operation.duration")
                .description("Profile operation latency")
                .tags("operation", operation.tag, "outcome", outcome.tag, "status", statusFamily.tag)
                .publishPercentileHistogram()
                .serviceLevelObjectives(
                        Duration.ofMillis(100), Duration.ofMillis(500), Duration.ofSeconds(2))
                .register(registry)
                .record(Math.max(0, elapsedNanos), TimeUnit.NANOSECONDS);
    }
}
