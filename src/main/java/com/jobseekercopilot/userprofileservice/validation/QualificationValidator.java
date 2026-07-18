package com.jobseekercopilot.userprofileservice.validation;

import com.jobseekercopilot.userprofileservice.model.Qualification;
import com.jobseekercopilot.userprofileservice.model.QualificationStatus;
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
                throw new IllegalArgumentException("COMPLETED qualifications must have a grade");
            }
            if (qualification.getDateAchieved() == null || qualification.getDateAchieved().trim().isEmpty()) {
                throw new IllegalArgumentException("COMPLETED qualifications must have a dateAchieved");
            }
            if (qualification.getExpectedCompletion() != null && !qualification.getExpectedCompletion().trim().isEmpty()) {
                throw new IllegalArgumentException("COMPLETED qualifications should not have expectedCompletion");
            }
        } else if (status == QualificationStatus.IN_PROGRESS) {
            if (qualification.getExpectedCompletion() == null || qualification.getExpectedCompletion().trim().isEmpty()) {
                throw new IllegalArgumentException("IN_PROGRESS qualifications must have expectedCompletion");
            }
            if (qualification.getGrade() != null && !qualification.getGrade().trim().isEmpty()) {
                throw new IllegalArgumentException("IN_PROGRESS qualifications should not have grade");
            }
            if (qualification.getDateAchieved() != null && !qualification.getDateAchieved().trim().isEmpty()) {
                throw new IllegalArgumentException("IN_PROGRESS qualifications should not have dateAchieved");
            }
        }
    }
}
