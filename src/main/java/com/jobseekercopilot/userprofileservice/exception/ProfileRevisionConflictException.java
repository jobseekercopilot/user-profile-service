package com.jobseekercopilot.userprofileservice.exception;

public class ProfileRevisionConflictException extends RuntimeException {

    public ProfileRevisionConflictException() {
        super("The supplied profile revision is stale");
    }
}
