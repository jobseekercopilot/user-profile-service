package com.jobseekercopilot.userprofileservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class ProfileDateValidator implements ConstraintValidator<ProfileDate, String> {

    private static final DateTimeFormatter YEAR_MONTH =
            DateTimeFormatter.ofPattern("uuuu-MM").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ISO_LOCAL_DATE
            .withResolverStyle(ResolverStyle.STRICT);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String candidate = value.strip();
        try {
            if (candidate.length() == 7) {
                int year = YearMonth.parse(candidate, YEAR_MONTH).getYear();
                return year >= 1900 && year <= 2099;
            }
            if (candidate.length() == 10) {
                int year = LocalDate.parse(candidate, FULL_DATE).getYear();
                return year >= 1900 && year <= 2099;
            }
            return false;
        } catch (DateTimeException exception) {
            return false;
        }
    }
}
