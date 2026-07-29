package com.jobseekercopilot.userprofileservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobseekercopilot.userprofileservice.exception.ProfileValidationException;
import com.jobseekercopilot.userprofileservice.model.evidence.DatePrecision;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceCategory;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceConfirmationState;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevision;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevisionCreator;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceWriteRequest;
import com.jobseekercopilot.userprofileservice.model.evidence.PartialDate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class EvidenceRequestProcessorTest {

    private final EvidenceRequestProcessor processor = new EvidenceRequestProcessor();

    @Test
    void acceptsConciseCategoryMinimumsIncludingFreelanceWithoutClientContext() {
        assertRevision(employment(false));

        EvidenceWriteRequest education = request(EvidenceCategory.EDUCATION);
        education.setProgrammeOrSubject("Computer Science");
        education.setInstitution("Open University");
        education.setResultOrStatus(EvidenceRequestProcessor.IN_PROGRESS);
        education.setEndDate(month(2027, 6));
        assertRevision(education);

        EvidenceWriteRequest qualification = request(EvidenceCategory.QUALIFICATION_TRAINING);
        qualification.setQualificationTitle("Cloud training");
        qualification.setIssuer("Training body");
        qualification.setResultOrStatus(EvidenceRequestProcessor.COMPLETED);
        qualification.setIssueDate(month(2026, 7));
        qualification.setExpiryDate(month(2029, 7));
        assertRevision(qualification);

        EvidenceWriteRequest project = request(EvidenceCategory.PROJECT);
        project.setDescription("Built an accessible job-search application.");
        assertRevision(project);

        EvidenceWriteRequest volunteering = request(EvidenceCategory.VOLUNTEERING);
        volunteering.setRoleTitle("Mentor");
        volunteering.setOrganisationContext("Community group");
        volunteering.setDescription("Helped learners practise Java.");
        assertRevision(volunteering);

        EvidenceWriteRequest freelance = request(EvidenceCategory.FREELANCE);
        freelance.setRoleTitle("Website developer");
        freelance.setDescription("Delivered a small business website.");
        assertRevision(freelance);

        EvidenceWriteRequest achievement = request(EvidenceCategory.ACHIEVEMENT);
        achievement.setDescription("Won the accessibility challenge.");
        achievement.setIssueDate(day(2026, 7, 15));
        assertRevision(achievement);

        EvidenceWriteRequest careerBreak = request(EvidenceCategory.CAREER_BREAK);
        assertRevision(careerBreak);

        EvidenceWriteRequest other = request(EvidenceCategory.OTHER);
        other.setDescription("Relevant additional experience.");
        assertRevision(other);
    }

    @Test
    void employmentRequiresStartAndEitherEndOrCurrent() {
        EvidenceWriteRequest missingStart = employment(false);
        missingStart.setStartDate(null);
        assertViolation(missingStart, "startDate", "REQUIRED_FOR_CATEGORY");

        EvidenceWriteRequest missingEnd = employment(false);
        missingEnd.setEndDate(null);
        assertViolation(missingEnd, "endDate", "END_DATE_OR_CURRENT_REQUIRED");
    }

    @Test
    void ongoingRejectsStaleEndDateAndProducesPresentFact() {
        EvidenceWriteRequest stale = employment(true);
        stale.setEndDate(month(2026, 7));
        assertViolation(stale, "endDate", "ONGOING_END_DATE");

        EvidenceWriteRequest current = employment(true);
        EvidenceRevision revision = assertRevision(current);

        assertNull(revision.getEndDate());
        assertTrue(revision.isOngoing());
        assertEquals(
                "Present",
                revision.getFacts().stream()
                        .filter(fact -> "END_DATE".equals(fact.getFactType()))
                        .findFirst()
                        .orElseThrow()
                        .getFactValue());
        assertFalse(revision.getFacts().stream()
                .filter(fact -> "END_DATE".equals(fact.getFactType()))
                .findFirst()
                .orElseThrow()
                .isNumericClaim());
    }

    @Test
    void educationAndQualificationRequireExactStatusAndOnlyRelevantDate() {
        EvidenceWriteRequest education = request(EvidenceCategory.EDUCATION);
        education.setProgrammeOrSubject("Music");
        education.setInstitution("University");
        education.setResultOrStatus("COMPLETED");
        education.setIssueDate(month(2020, 6));
        assertViolation(
                education, "resultOrStatus", "COMPLETION_STATUS_REQUIRED");

        education.setResultOrStatus(EvidenceRequestProcessor.COMPLETED);
        education.setIssueDate(null);
        assertViolation(education, "issueDate", "REQUIRED_FOR_CATEGORY");

        education.setIssueDate(month(2020, 6));
        education.setEndDate(month(2020, 7));
        assertViolation(education, "endDate", "NOT_APPLICABLE_FOR_CATEGORY");

        EvidenceWriteRequest qualification = request(EvidenceCategory.QUALIFICATION_TRAINING);
        qualification.setQualificationTitle("Certificate");
        qualification.setIssuer("Issuer");
        qualification.setResultOrStatus(EvidenceRequestProcessor.IN_PROGRESS);
        qualification.setEndDate(month(2027, 3));
        qualification.setExpiryDate(month(2030, 3));
        assertViolation(qualification, "expiryDate", "NOT_APPLICABLE_FOR_CATEGORY");
    }

    @Test
    void rejectsImpossibleDateChronologyAtEveryPrecision() {
        EvidenceWriteRequest employment = employment(false);
        employment.setStartDate(day(2026, 8, 1));
        employment.setEndDate(month(2026, 7));
        assertViolation(employment, "endDate", "END_BEFORE_START");

        EvidenceWriteRequest qualification = request(EvidenceCategory.QUALIFICATION_TRAINING);
        qualification.setQualificationTitle("Certificate");
        qualification.setIssuer("Issuer");
        qualification.setResultOrStatus(EvidenceRequestProcessor.COMPLETED);
        qualification.setIssueDate(month(2027, 3));
        qualification.setExpiryDate(new PartialDate(DatePrecision.YEAR, 2026, null, null));
        assertViolation(qualification, "expiryDate", "EXPIRY_BEFORE_ISSUE");
    }

    @Test
    void boundsNarrativeFactsAndTotalGeneratedFactCount() {
        EvidenceWriteRequest oversized = request(EvidenceCategory.OTHER);
        oversized.setDescription("x".repeat(2001));
        assertViolation(oversized, "description", "SIZE");

        EvidenceWriteRequest tooManyFacts = request(EvidenceCategory.OTHER);
        tooManyFacts.setDescription("Bounded description");
        tooManyFacts.setDemonstratedSkills(IntStream.range(0, 50)
                .mapToObj(index -> "Skill " + index)
                .toList());
        assertViolation(tooManyFacts, "demonstratedSkills", "FACT_LIMIT_EXCEEDED");
    }

    @Test
    void careerBreakReasonIsRetainedPrivatelyButNeverEmittedAsFact() {
        EvidenceWriteRequest careerBreak = request(EvidenceCategory.CAREER_BREAK);
        careerBreak.setCareerBreakReason("Private family circumstances");

        EvidenceRevision revision = assertRevision(careerBreak);

        assertEquals("Private family circumstances", revision.getCareerBreakReason());
        assertTrue(revision.getFacts().stream()
                .noneMatch(fact -> fact.getFactType().contains("CAREER")
                        || fact.getFactValue().contains("family circumstances")));
    }

    @Test
    void confirmingLegacyQualificationCanCreateNewSemanticallyValidRevisionWithoutMutation() {
        EvidenceEntry entry = new EvidenceEntry();
        entry.setCategory(EvidenceCategory.QUALIFICATION_TRAINING);
        EvidenceRevision legacy = new EvidenceRevision();
        legacy.setHeading("Cloud certificate");
        legacy.setQualificationTitle("Cloud certificate");
        legacy.setIssuer("Training provider");
        legacy.setResultOrStatus("COMPLETED — Distinction");
        legacy.setIssueDate(month(2025, 6));
        legacy.setEndDate(month(2025, 7));
        legacy.setCreatedBy(EvidenceRevisionCreator.LEGACY_MIGRATION);

        EvidenceWriteRequest translated = processor.fromRevision(entry, legacy);
        EvidenceRevision confirmed = processor.revision(
                translated,
                2,
                EvidenceConfirmationState.USER_CONFIRMED,
                EvidenceRevisionCreator.USER);

        assertEquals("COMPLETED — Distinction", legacy.getResultOrStatus());
        assertEquals(7, legacy.getEndDate().getMonth());
        assertEquals(EvidenceRequestProcessor.COMPLETED, confirmed.getResultOrStatus());
        assertNull(confirmed.getEndDate());
        assertEquals(month(2025, 6).getYear(), confirmed.getIssueDate().getYear());
    }

    @Test
    void confirmingLegacyCurrentEvidenceClearsStaleEndDateWithoutMutatingSource() {
        EvidenceEntry entry = new EvidenceEntry();
        entry.setCategory(EvidenceCategory.EMPLOYMENT);
        EvidenceRevision legacy = new EvidenceRevision();
        legacy.setHeading("Software Engineer");
        legacy.setRoleTitle("Software Engineer");
        legacy.setOrganisationContext("Example Ltd");
        legacy.setStartDate(month(2025, 1));
        legacy.setEndDate(month(2026, 1));
        legacy.setOngoing(true);
        legacy.setCreatedBy(EvidenceRevisionCreator.LEGACY_MIGRATION);

        EvidenceWriteRequest translated = processor.fromRevision(entry, legacy);
        EvidenceRevision confirmed = processor.revision(
                translated,
                2,
                EvidenceConfirmationState.USER_CONFIRMED,
                EvidenceRevisionCreator.USER);

        assertEquals(1, legacy.getEndDate().getMonth());
        assertNull(confirmed.getEndDate());
        assertTrue(confirmed.getFacts().stream()
                .anyMatch(fact -> "END_DATE".equals(fact.getFactType())
                        && "Present".equals(fact.getFactValue())));
    }

    @Test
    void confirmingLegacyInProgressQualificationKeepsOnlyExpectedCompletion() {
        EvidenceEntry entry = new EvidenceEntry();
        entry.setCategory(EvidenceCategory.QUALIFICATION_TRAINING);
        EvidenceRevision legacy = new EvidenceRevision();
        legacy.setHeading("Cloud certificate");
        legacy.setQualificationTitle("Cloud certificate");
        legacy.setIssuer("Training provider");
        legacy.setResultOrStatus("IN_PROGRESS");
        legacy.setIssueDate(month(2025, 1));
        legacy.setEndDate(month(2026, 6));
        legacy.setExpiryDate(month(2028, 1));
        legacy.setCreatedBy(EvidenceRevisionCreator.LEGACY_MIGRATION);

        EvidenceWriteRequest translated = processor.fromRevision(entry, legacy);
        EvidenceRevision confirmed = processor.revision(
                translated,
                2,
                EvidenceConfirmationState.USER_CONFIRMED,
                EvidenceRevisionCreator.USER);

        assertEquals(1, legacy.getIssueDate().getMonth());
        assertEquals(EvidenceRequestProcessor.IN_PROGRESS, confirmed.getResultOrStatus());
        assertNull(confirmed.getIssueDate());
        assertNull(confirmed.getExpiryDate());
        assertEquals(6, confirmed.getEndDate().getMonth());
    }

    private EvidenceWriteRequest employment(boolean ongoing) {
        EvidenceWriteRequest request = request(EvidenceCategory.EMPLOYMENT);
        request.setRoleTitle("Software Engineer");
        request.setOrganisationContext("Example Ltd");
        request.setStartDate(month(2025, 1));
        request.setOngoing(ongoing);
        if (!ongoing) {
            request.setEndDate(month(2026, 6));
        }
        return request;
    }

    private EvidenceWriteRequest request(EvidenceCategory category) {
        EvidenceWriteRequest request = new EvidenceWriteRequest();
        request.setCategory(category);
        request.setHeading("Evidence heading");
        return request;
    }

    private EvidenceRevision assertRevision(EvidenceWriteRequest request) {
        return processor.revision(
                request,
                1,
                EvidenceConfirmationState.DRAFT,
                EvidenceRevisionCreator.USER);
    }

    private void assertViolation(EvidenceWriteRequest request, String field, String code) {
        ProfileValidationException exception = assertThrows(
                ProfileValidationException.class, () -> assertRevision(request));
        assertEquals(field, exception.getField());
        assertEquals(code, exception.getCode());
    }

    private PartialDate month(int year, int month) {
        return new PartialDate(DatePrecision.MONTH, year, month, null);
    }

    private PartialDate day(int year, int month, int day) {
        return new PartialDate(DatePrecision.DAY, year, month, day);
    }
}
