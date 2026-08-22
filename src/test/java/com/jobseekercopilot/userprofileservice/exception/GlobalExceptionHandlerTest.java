package com.jobseekercopilot.userprofileservice.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void profileWriteConflictIsStableRetriableAndDoesNotLeakDatabaseDetails() {
        var response = handler.handleProfileWriteConflict();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("PROFILE_WRITE_CONFLICT", response.getBody().code());
        assertEquals("The profile changed concurrently; retry the request.", response.getBody().message());
        assertFalse(response.getBody().toString().contains("constraint"));
    }
}
