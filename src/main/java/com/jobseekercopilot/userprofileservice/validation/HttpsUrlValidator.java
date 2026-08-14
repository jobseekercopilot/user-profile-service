package com.jobseekercopilot.userprofileservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;

public class HttpsUrlValidator implements ConstraintValidator<HttpsUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isValidValue(value);
    }

    public static boolean isValidValue(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            String normalized = value.trim();
            URI uri = new URI(normalized);
            return "https".equals(uri.getScheme())
                    && uri.isAbsolute()
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getUserInfo() == null
                    && normalized.codePoints().noneMatch(Character::isISOControl);
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
