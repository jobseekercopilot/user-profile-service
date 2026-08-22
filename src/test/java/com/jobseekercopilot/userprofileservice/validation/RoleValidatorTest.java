package com.jobseekercopilot.userprofileservice.validation;

import com.jobseekercopilot.userprofileservice.model.Role;
import com.jobseekercopilot.userprofileservice.model.RoleStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class RoleValidatorTest {

    private final RoleValidator validator = new RoleValidator();

    @Test
    void validate_ShouldPass_WhenRoleIsNull() {
        validator.validate(null);
    }

    @Test
    void validate_ShouldThrow_WhenCurrentHasEndDate() throws Exception {
        Role role = new Role();
        setField(role, "jobTitle", "Software Engineer");
        setField(role, "employer", "Tech Corp");
        setField(role, "status", RoleStatus.CURRENT);
        setField(role, "startDate", "2022-01");
        setField(role, "endDate", "2023-12");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(role));
        assertEquals("CURRENT roles should not have endDate", exception.getMessage());
    }

    @Test
    void validate_ShouldPass_WhenCurrentIsValid() throws Exception {
        Role role = new Role();
        setField(role, "jobTitle", "Software Engineer");
        setField(role, "employer", "Tech Corp");
        setField(role, "status", RoleStatus.CURRENT);
        setField(role, "startDate", "2022-01");

        validator.validate(role);
    }

    @Test
    void validate_ShouldThrow_WhenPreviousRoleHasNoEndDate() throws Exception {
        Role role = new Role();
        setField(role, "jobTitle", "Junior Developer");
        setField(role, "employer", "Startup Inc");
        setField(role, "status", RoleStatus.PREVIOUS_ROLE);
        setField(role, "startDate", "2020-06");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(role));
        assertEquals("PREVIOUS_ROLE must have endDate", exception.getMessage());
    }

    @Test
    void validate_ShouldPass_WhenPreviousRoleIsValid() throws Exception {
        Role role = new Role();
        setField(role, "jobTitle", "Junior Developer");
        setField(role, "employer", "Startup Inc");
        setField(role, "status", RoleStatus.PREVIOUS_ROLE);
        setField(role, "startDate", "2020-06");
        setField(role, "endDate", "2022-01");

        validator.validate(role);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}