package com.jobseekercopilot.userprofileservice.exception;

public class EvidenceNotFoundException extends RuntimeException {

    public EvidenceNotFoundException() {
        super("Evidence entry not found");
    }
}
