package com.jobseekercopilot.userprofileservice.exception;

public class ProfileValidationException extends IllegalArgumentException {

    private final String field;
    private final String code;

    public ProfileValidationException(String field, String code, String message) {
        super(message);
        this.field = field;
        this.code = code;
    }

    public String getField() {
        return field;
    }

    public String getCode() {
        return code;
    }
}
