package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.exception.ProfileValidationException;
import com.jobseekercopilot.userprofileservice.model.evidence.DatePrecision;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceConfirmationState;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceFact;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevision;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevisionCreator;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceWriteRequest;
import com.jobseekercopilot.userprofileservice.model.evidence.PartialDate;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class EvidenceRequestProcessor {

    private static final Pattern HTML = Pattern.compile("<\\s*/?\\s*[a-zA-Z][^>]*>");

    public EvidenceRevision revision(
            EvidenceWriteRequest request,
            int revisionNumber,
            EvidenceConfirmationState confirmationState,
            EvidenceRevisionCreator creator) {
        normalizeAndValidate(request);
        EvidenceRevision revision = new EvidenceRevision();
        revision.setRevisionNumber(revisionNumber);
        revision.setConfirmationState(confirmationState);
        revision.setHeading(request.getHeading());
        revision.setOrganisationContext(request.getOrganisationContext());
        revision.setRoleTitle(request.getRoleTitle());
        revision.setProgrammeOrSubject(request.getProgrammeOrSubject());
        revision.setInstitution(request.getInstitution());
        revision.setQualificationTitle(request.getQualificationTitle());
        revision.setIssuer(request.getIssuer());
        revision.setResultOrStatus(request.getResultOrStatus());
        revision.setProjectRole(request.getProjectRole());
        revision.setDescription(request.getDescription());
        revision.setResponsibilities(request.getResponsibilities());
        revision.setAchievements(request.getAchievements());
        revision.setCareerBreakReason(request.getCareerBreakReason());
        revision.setPrivateCredentialIdentifier(request.getPrivateCredentialIdentifier());
        revision.setOngoing(request.isOngoing());
        revision.setStartDate(copy(request.getStartDate()));
        revision.setEndDate(copy(request.getEndDate()));
        revision.setIssueDate(copy(request.getIssueDate()));
        revision.setExpiryDate(copy(request.getExpiryDate()));
        revision.setDemonstratedSkills(new ArrayList<>(request.getDemonstratedSkills()));
        revision.setSupportingLinks(new ArrayList<>(request.getSupportingLinks()));
        revision.setCreatedBy(creator);
        addFacts(revision, request);
        revision.setContentDigest(digest(request));
        return revision;
    }

    public EvidenceWriteRequest fromRevision(
            com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry entry,
            EvidenceRevision revision) {
        EvidenceWriteRequest request = new EvidenceWriteRequest();
        request.setCategory(entry.getCategory());
        request.setHeading(revision.getHeading());
        request.setOrganisationContext(revision.getOrganisationContext());
        request.setRoleTitle(revision.getRoleTitle());
        request.setProgrammeOrSubject(revision.getProgrammeOrSubject());
        request.setInstitution(revision.getInstitution());
        request.setQualificationTitle(revision.getQualificationTitle());
        request.setIssuer(revision.getIssuer());
        request.setResultOrStatus(revision.getResultOrStatus());
        request.setProjectRole(revision.getProjectRole());
        request.setDescription(revision.getDescription());
        request.setResponsibilities(revision.getResponsibilities());
        request.setAchievements(revision.getAchievements());
        request.setCareerBreakReason(revision.getCareerBreakReason());
        request.setPrivateCredentialIdentifier(revision.getPrivateCredentialIdentifier());
        request.setOngoing(revision.isOngoing());
        request.setStartDate(copy(revision.getStartDate()));
        request.setEndDate(copy(revision.getEndDate()));
        request.setIssueDate(copy(revision.getIssueDate()));
        request.setExpiryDate(copy(revision.getExpiryDate()));
        request.setDemonstratedSkills(new ArrayList<>(revision.getDemonstratedSkills()));
        request.setSupportingLinks(new ArrayList<>(revision.getSupportingLinks()));
        return request;
    }

    private void normalizeAndValidate(EvidenceWriteRequest request) {
        request.setHeading(normalize(request.getHeading()));
        request.setOrganisationContext(normalize(request.getOrganisationContext()));
        request.setRoleTitle(normalize(request.getRoleTitle()));
        request.setProgrammeOrSubject(normalize(request.getProgrammeOrSubject()));
        request.setInstitution(normalize(request.getInstitution()));
        request.setQualificationTitle(normalize(request.getQualificationTitle()));
        request.setIssuer(normalize(request.getIssuer()));
        request.setResultOrStatus(normalize(request.getResultOrStatus()));
        request.setProjectRole(normalize(request.getProjectRole()));
        request.setDescription(normalize(request.getDescription()));
        request.setResponsibilities(normalize(request.getResponsibilities()));
        request.setAchievements(normalize(request.getAchievements()));
        request.setCareerBreakReason(normalize(request.getCareerBreakReason()));
        request.setPrivateCredentialIdentifier(normalize(request.getPrivateCredentialIdentifier()));
        request.setDemonstratedSkills(normalizeList(request.getDemonstratedSkills()));
        request.setSupportingLinks(normalizeList(request.getSupportingLinks()));

        if (request.getHeading() == null) {
            invalid("heading", "NOT_BLANK");
        }
        checkPlainText("heading", request.getHeading());
        checkPlainText("organisationContext", request.getOrganisationContext());
        checkPlainText("roleTitle", request.getRoleTitle());
        checkPlainText("programmeOrSubject", request.getProgrammeOrSubject());
        checkPlainText("institution", request.getInstitution());
        checkPlainText("qualificationTitle", request.getQualificationTitle());
        checkPlainText("issuer", request.getIssuer());
        checkPlainText("resultOrStatus", request.getResultOrStatus());
        checkPlainText("projectRole", request.getProjectRole());
        checkPlainText("description", request.getDescription());
        checkPlainText("responsibilities", request.getResponsibilities());
        checkPlainText("achievements", request.getAchievements());
        checkPlainText("careerBreakReason", request.getCareerBreakReason());
        checkPlainText("privateCredentialIdentifier", request.getPrivateCredentialIdentifier());
        request.getDemonstratedSkills().forEach(value -> checkPlainText("demonstratedSkills", value));
        request.getSupportingLinks().forEach(this::validateLink);
        validateDate("startDate", request.getStartDate());
        validateDate("endDate", request.getEndDate());
        validateDate("issueDate", request.getIssueDate());
        validateDate("expiryDate", request.getExpiryDate());
        if (request.isOngoing() && request.getEndDate() != null) {
            invalid("endDate", "ONGOING_END_DATE");
        }
        validateCategory(request);
    }

    private void validateCategory(EvidenceWriteRequest request) {
        if (request.getCategory() == null) {
            invalid("category", "NOT_NULL");
        }
        switch (request.getCategory()) {
            case EMPLOYMENT, FREELANCE -> {
                require("roleTitle", request.getRoleTitle());
                require("organisationContext", request.getOrganisationContext());
            }
            case EDUCATION -> {
                require("programmeOrSubject", request.getProgrammeOrSubject());
                require("institution", request.getInstitution());
            }
            case QUALIFICATION_TRAINING -> {
                require("qualificationTitle", request.getQualificationTitle());
                require("issuer", request.getIssuer());
            }
            case VOLUNTEERING -> {
                require("roleTitle", request.getRoleTitle());
                require("organisationContext", request.getOrganisationContext());
            }
            case ACHIEVEMENT -> {
                if (request.getAchievements() == null && request.getDescription() == null) {
                    invalid("achievements", "REQUIRED_FOR_CATEGORY");
                }
            }
            case OTHER -> require("description", request.getDescription());
            case PROJECT, CAREER_BREAK -> {
                // The bounded common heading is sufficient; category-specific context is optional.
            }
        }
    }

    private void validateDate(String field, PartialDate date) {
        if (date == null) {
            return;
        }
        if (date.getPrecision() == null || date.getYear() == null) {
            invalid(field, "INCOMPLETE_PARTIAL_DATE");
        }
        if (date.getYear() < 1900 || date.getYear() > 2200) {
            invalid(field, "INVALID_YEAR");
        }
        try {
            if (date.getPrecision() == DatePrecision.YEAR) {
                if (date.getMonth() != null || date.getDay() != null) {
                    invalid(field, "INVALID_PRECISION");
                }
            } else if (date.getPrecision() == DatePrecision.MONTH) {
                if (date.getMonth() == null || date.getDay() != null) {
                    invalid(field, "INVALID_PRECISION");
                }
                LocalDate.of(date.getYear(), date.getMonth(), 1);
            } else {
                if (date.getMonth() == null || date.getDay() == null) {
                    invalid(field, "INVALID_PRECISION");
                }
                LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
            }
        } catch (java.time.DateTimeException exception) {
            invalid(field, "INVALID_DATE");
        }
    }

    private void validateLink(String value) {
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getHost().isBlank()
                    || uri.getUserInfo() != null) {
                invalid("supportingLinks", "HTTPS_URL_REQUIRED");
            }
        } catch (URISyntaxException exception) {
            invalid("supportingLinks", "INVALID_URL");
        }
    }

    private void checkPlainText(String field, String value) {
        if (value != null && HTML.matcher(value).find()) {
            invalid(field, "PLAIN_TEXT_ONLY");
        }
    }

    private void require(String field, String value) {
        if (value == null) {
            invalid(field, "REQUIRED_FOR_CATEGORY");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .map(this::normalize)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private String digest(EvidenceWriteRequest request) {
        StringBuilder canonical = new StringBuilder("evidence-v1|");
        append(canonical, request.getCategory());
        append(canonical, request.getHeading());
        append(canonical, request.getOrganisationContext());
        append(canonical, request.getRoleTitle());
        append(canonical, request.getProgrammeOrSubject());
        append(canonical, request.getInstitution());
        append(canonical, request.getQualificationTitle());
        append(canonical, request.getIssuer());
        append(canonical, request.getResultOrStatus());
        append(canonical, request.getProjectRole());
        append(canonical, request.getDescription());
        append(canonical, request.getResponsibilities());
        append(canonical, request.getAchievements());
        append(canonical, request.getCareerBreakReason());
        append(canonical, request.getPrivateCredentialIdentifier());
        append(canonical, request.isOngoing());
        append(canonical, partialDateValue(request.getStartDate()));
        append(canonical, partialDateValue(request.getEndDate()));
        append(canonical, partialDateValue(request.getIssueDate()));
        append(canonical, partialDateValue(request.getExpiryDate()));
        request.getDemonstratedSkills().forEach(value -> append(canonical, value));
        request.getSupportingLinks().forEach(value -> append(canonical, value));
        return DigestSupport.sha256(canonical.toString());
    }

    private void addFacts(EvidenceRevision revision, EvidenceWriteRequest request) {
        addFact(revision, "HEADING", request.getHeading());
        addFact(revision, "ORGANISATION_CONTEXT", request.getOrganisationContext());
        addFact(revision, "ROLE_TITLE", request.getRoleTitle());
        addFact(revision, "PROGRAMME_OR_SUBJECT", request.getProgrammeOrSubject());
        addFact(revision, "INSTITUTION", request.getInstitution());
        addFact(revision, "QUALIFICATION_TITLE", request.getQualificationTitle());
        addFact(revision, "ISSUER", request.getIssuer());
        addFact(revision, "RESULT_OR_STATUS", request.getResultOrStatus());
        addFact(revision, "PROJECT_ROLE", request.getProjectRole());
        addFact(revision, "DESCRIPTION", request.getDescription());
        addFact(revision, "RESPONSIBILITIES", request.getResponsibilities());
        addFact(revision, "ACHIEVEMENTS", request.getAchievements());
        addFact(revision, "START_DATE", partialDateValue(request.getStartDate()));
        addFact(revision, "END_DATE", partialDateValue(request.getEndDate()));
        addFact(revision, "ISSUE_DATE", partialDateValue(request.getIssueDate()));
        addFact(revision, "EXPIRY_DATE", partialDateValue(request.getExpiryDate()));
        request.getDemonstratedSkills().forEach(value -> addFact(revision, "DEMONSTRATED_SKILL", value));
        // Private credential identifiers, career-break reasons and supporting links are not facts by default.
    }

    private void addFact(EvidenceRevision revision, String type, String value) {
        if (value == null) {
            return;
        }
        EvidenceFact fact = new EvidenceFact();
        fact.setFactType(type);
        fact.setFactValue(value);
        fact.setNumericClaim(value.chars().anyMatch(Character::isDigit));
        revision.addFact(fact);
    }

    private PartialDate copy(PartialDate date) {
        return date == null
                ? null
                : new PartialDate(date.getPrecision(), date.getYear(), date.getMonth(), date.getDay());
    }

    private String partialDateValue(PartialDate date) {
        if (date == null || date.getPrecision() == null || date.getYear() == null) {
            return null;
        }
        return switch (date.getPrecision()) {
            case YEAR -> "%04d".formatted(date.getYear());
            case MONTH -> "%04d-%02d".formatted(date.getYear(), date.getMonth());
            case DAY -> "%04d-%02d-%02d".formatted(date.getYear(), date.getMonth(), date.getDay());
        };
    }

    private void append(StringBuilder canonical, Object value) {
        DigestSupport.append(canonical, value);
    }

    private void invalid(String field, String code) {
        throw new ProfileValidationException(field, code, "Evidence input is invalid");
    }
}
