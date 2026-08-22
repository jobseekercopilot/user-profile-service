package com.jobseekercopilot.userprofileservice.validation;

import com.jobseekercopilot.userprofileservice.model.Role;
import com.jobseekercopilot.userprofileservice.model.RoleStatus;
import com.jobseekercopilot.userprofileservice.exception.ProfileValidationException;
import org.springframework.stereotype.Component;

@Component
public class RoleValidator {

    public void validate(Role role) {
        if (role == null) {
            return;
        }

        RoleStatus status = role.getStatus();

        if (status == RoleStatus.CURRENT) {
            if (role.getEndDate() != null && !role.getEndDate().trim().isEmpty()) {
                throw new ProfileValidationException("roles.endDate", "FORBIDDEN_FOR_STATUS",
                        "CURRENT roles should not have endDate");
            }
        } else if (status == RoleStatus.PREVIOUS_ROLE) {
            if (role.getEndDate() == null || role.getEndDate().trim().isEmpty()) {
                throw new ProfileValidationException("roles.endDate", "REQUIRED_FOR_STATUS",
                        "PREVIOUS_ROLE must have endDate");
            }
        }
    }
}
