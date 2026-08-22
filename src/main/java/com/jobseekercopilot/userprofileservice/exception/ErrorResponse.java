package com.jobseekercopilot.userprofileservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String schemaVersion,
        String code,
        String message,
        String correlationId,
        Instant timestamp,
        List<FieldViolation> violations) {

    public static final String SCHEMA_VERSION = "1";

    public ErrorResponse(String code, String message, String correlationId) {
        this(SCHEMA_VERSION, code, message, correlationId, Instant.now(), List.of());
    }

    public ErrorResponse(String code, String message, String correlationId, List<FieldViolation> violations) {
        this(SCHEMA_VERSION, code, message, correlationId, Instant.now(), violations);
    }
}
