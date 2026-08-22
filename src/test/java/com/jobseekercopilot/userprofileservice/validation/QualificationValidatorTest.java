package com.jobseekercopilot.userprofileservice.validation;

import com.jobseekercopilot.userprofileservice.model.Qualification;
import com.jobseekercopilot.userprofileservice.model.QualificationStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class QualificationValidatorTest {

    private final QualificationValidator validator = new QualificationValidator();

    @Test
    void validate_ShouldPass_WhenQualificationIsNull() {
        validator.validate(null);
    }

    @Test
    void validate_ShouldThrow_WhenCompletedHasNoGrade() throws Exception {
        Qualification qualification = new Qualification();
        setField(qualification, "qualificationName", "BSc Computer Science");
        setField(qualification, "issuingBody", "University");
        setField(qualification, "status", QualificationStatus.COMPLETED);
        setField(qualification, "dateAchieved", "2023-06");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(qualification));
        assertEquals("COMPLETED qualifications must have a grade", exception.getMessage());
    }

    @Test
    void validate_ShouldThrow_WhenCompletedHasNoDateAchieved() throws Exception {
        Qualification qualification = new Qualification();
        setField(qualification, "qualificationName", "BSc Computer Science");
        setField(qualification, "issuingBody", "University");
        setField(qualification, "status", QualificationStatus.COMPLETED);
        setField(qualification, "grade", "First Class");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(qualification));
        assertEquals("COMPLETED qualifications must have a dateAchieved", exception.getMessage());
    }

    @Test
    void validate_ShouldThrow_WhenCompletedHasExpectedCompletion() throws Exception {
        Qualification qualification = new Qualification();
        setField(qualification, "qualificationName", "BSc Computer Science");
        setField(qualification, "issuingBody", "University");
        setField(qualification, "status", QualificationStatus.COMPLETED);
        setField(qualification, "grade", "First Class");
        setField(qualification, "dateAchieved", "2023-06");
        setField(qualification, "expectedCompletion", "2024-06");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(qualification));
        assertEquals("COMPLETED qualifications should not have expectedCompletion", exception.getMessage());
    }

    @Test
    void validate_ShouldPass_WhenCompletedIsValid() throws Exception {
        Qualification qualification = new Qualification();
        setField(qualification, "qualificationName", "BSc Computer Science");
        setField(qualification, "issuingBody", "University");
        setField(qualification, "status", QualificationStatus.COMPLETED);
        setField(qualification, "grade", "First Class");
        setField(qualification, "dateAchieved", "2023-06");

        validator.validate(qualification);
    }

    @Test
    void validate_ShouldThrow_WhenInProgressHasNoExpectedCompletion() throws Exception {
        Qualification qualification = new Qualification();
        setField(qualification, "qualificationName", "MSc Data Science");
        setField(qualification, "issuingBody", "University");
        setField(qualification, "status", QualificationStatus.IN_PROGRESS);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(qualification));
        assertEquals("IN_PROGRESS qualifications must have expectedCompletion", exception.getMessage());
    }

    @Test
    void validate_ShouldThrow_WhenInProgressHasGrade() throws Exception {
        Qualification qualification = new Qualification();
        setField(qualification, "qualificationName", "MSc Data Science");
        setField(qualification, "issuingBody", "University");
        setField(qualification, "status", QualificationStatus.IN_PROGRESS);
        setField(qualification, "expectedCompletion", "2024-06");
        setField(qualification, "grade", "Pending");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(qualification));
        assertEquals("IN_PROGRESS qualifications should not have grade", exception.getMessage());
    }

    @Test
    void validate_ShouldThrow_WhenInProgressHasDateAchieved() throws Exception {
        Qualification qualification = new Qualification();
        setField(qualification, "qualificationName", "MSc Data Science");
        setField(qualification, "issuingBody", "University");
        setField(qualification, "status", QualificationStatus.IN_PROGRESS);
        setField(qualification, "expectedCompletion", "2024-06");
        setField(qualification, "dateAchieved", "2023-06");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(qualification));
        assertEquals("IN_PROGRESS qualifications should not have dateAchieved", exception.getMessage());
    }

    @Test
    void validate_ShouldPass_WhenInProgressIsValid() throws Exception {
        Qualification qualification = new Qualification();
        setField(qualification, "qualificationName", "MSc Data Science");
        setField(qualification, "issuingBody", "University");
        setField(qualification, "status", QualificationStatus.IN_PROGRESS);
        setField(qualification, "expectedCompletion", "2024-06");

        validator.validate(qualification);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}