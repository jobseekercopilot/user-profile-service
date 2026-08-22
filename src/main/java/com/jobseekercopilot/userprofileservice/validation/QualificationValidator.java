package com.jobseekercopilot.userprofileservice.validation;

import com.jobseekercopilot.userprofileservice.model.Qualification;
import com.jobseekercopilot.userprofileservice.model.QualificationStatus;
import com.jobseekercopilot.userprofileservice.exception.ProfileValidationException;
import org.springframework.stereotype.Component;

@Component
public class QualificationValidator {

    public void validate(Qualification qualification) {
        if (qualification == null) {
            return;
        }

        QualificationStatus status = qualification.getStatus();

        if (status == QualificationStatus.COMPLETED) {
            if (qualification.getGrade() == null || qualification.getGrade().trim().isEmpty()) {
                throw invalid("qualifications.grade", "REQUIRED_FOR_STATUS",
                        "COMPLETED qualifications must have a grade");
            }
            if (qualification.getDateAchieved() == null || qualification.getDateAchieved().trim().isEmpty()) {
                throw invalid("qualifications.dateAchieved", "REQUIRED_FOR_STATUS",
                        "COMPLETED qualifications must have a dateAchieved");
            }
            if (qualification.getExpectedCompletion() != null && !qualification.getExpectedCompletion().trim().isEmpty()) {
                throw invalid("qualifications.expectedCompletion", "FORBIDDEN_FOR_STATUS",
                        "COMPLETED qualifications should not have expectedCompletion");
            }
        } else if (status == QualificationStatus.IN_PROGRESS) {
            if (qualification.getExpectedCompletion() == null || qualification.getExpectedCompletion().trim().isEmpty()) {
                throw invalid("qualifications.expectedCompletion", "REQUIRED_FOR_STATUS",
                        "IN_PROGRESS qualifications must have expectedCompletion");
            }
            if (qualification.getGrade() != null && !qualification.getGrade().trim().isEmpty()) {
                throw invalid("qualifications.grade", "FORBIDDEN_FOR_STATUS",
                        "IN_PROGRESS qualifications should not have grade");
            }
            if (qualification.getDateAchieved() != null && !qualification.getDateAchieved().trim().isEmpty()) {
                throw invalid("qualifications.dateAchieved", "FORBIDDEN_FOR_STATUS",
                        "IN_PROGRESS qualifications should not have dateAchieved");
            }
        }
    }

    private ProfileValidationException invalid(String field, String code, String message) {
        return new ProfileValidationException(field, code, message);
    }
}
