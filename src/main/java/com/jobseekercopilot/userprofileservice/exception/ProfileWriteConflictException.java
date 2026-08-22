package com.jobseekercopilot.userprofileservice.exception;

public class ProfileWriteConflictException extends RuntimeException {

    public ProfileWriteConflictException(Throwable cause) {
        super("Concurrent profile write conflict", cause);
    }
}
