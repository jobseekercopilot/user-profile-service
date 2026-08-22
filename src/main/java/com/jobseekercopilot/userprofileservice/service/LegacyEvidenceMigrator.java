package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.model.Qualification;
import com.jobseekercopilot.userprofileservice.model.Role;
import com.jobseekercopilot.userprofileservice.model.RoleStatus;
import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.evidence.DatePrecision;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceCategory;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceConfirmationState;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceFact;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceLifecycle;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevision;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevisionCreator;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceVisibility;
import com.jobseekercopilot.userprofileservice.model.evidence.PartialDate;
import com.jobseekercopilot.userprofileservice.repository.EvidenceEntryRepository;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyEvidenceMigrator {

    private static final Logger log = LoggerFactory.getLogger(LegacyEvidenceMigrator.class);

    private final UserProfileRepository profileRepository;
    private final EvidenceEntryRepository evidenceRepository;
    private final ProfileDigestCalculator profileDigestCalculator;

    public LegacyEvidenceMigrator(
            UserProfileRepository profileRepository,
            EvidenceEntryRepository evidenceRepository,
            ProfileDigestCalculator profileDigestCalculator) {
        this.profileRepository = profileRepository;
        this.evidenceRepository = evidenceRepository;
        this.profileDigestCalculator = profileDigestCalculator;
    }

    @Transactional
    public int migrateForOwner(String userId) {
        return profileRepository.findByUserId(userId)
                .map(this::migrateProfile)
                .orElse(0);
    }

    int migrateProfile(UserProfile profile) {
        backfillProfileRevision(profile);
        int migrated = 0;
        if (profile.getRoles() != null) {
            for (Role role : profile.getRoles()) {
                String sourceHash = roleSourceHash(role);
                migrated += createIfMissing(
                        profile,
                        "LEGACY_ROLE:" + sourceHash,
                        sourceHash,
                        employmentEntry(profile, role, sourceHash));
            }
        }
        if (profile.getQualifications() != null) {
            for (Qualification qualification : profile.getQualifications()) {
                String sourceHash = qualificationSourceHash(qualification);
                migrated += createIfMissing(
                        profile,
                        "LEGACY_QUALIFICATION:" + sourceHash,
                        sourceHash,
                        qualificationEntry(profile, qualification, sourceHash));
            }
        }
        if (migrated > 0) {
            log.info("Legacy profile evidence migrated entriesCount={}", migrated);
        }
        return migrated;
    }

    private void backfillProfileRevision(UserProfile profile) {
        boolean changed = false;
        if (profile.getRevisionId() == null) {
            profile.setRevisionId(UUID.randomUUID().toString());
            changed = true;
        }
        if (profile.getContentDigest() == null) {
            profile.setContentDigest(profileDigestCalculator.digest(profile));
            changed = true;
        }
        if (changed) {
            profileRepository.save(profile);
        }
    }

    private int createIfMissing(
            UserProfile profile,
            String migrationKey,
            String sourceHash,
            EvidenceEntry candidate) {
        if (evidenceRepository.existsByUserProfileIdAndMigrationKey(profile.getId(), migrationKey)) {
            return 0;
        }
        candidate.setMigrationKey(migrationKey);
        candidate.setSourceHash(sourceHash);
        try {
            evidenceRepository.saveAndFlush(candidate);
            return 1;
        } catch (DataIntegrityViolationException conflict) {
            if (evidenceRepository.existsByUserProfileIdAndMigrationKey(profile.getId(), migrationKey)) {
                return 0;
            }
            throw conflict;
        }
    }

    private EvidenceEntry employmentEntry(UserProfile profile, Role role, String contentDigest) {
        EvidenceEntry entry = baseEntry(profile, EvidenceCategory.EMPLOYMENT);
        EvidenceRevision revision = baseRevision(role.getJobTitle(), contentDigest);
        revision.setOrganisationContext(role.getEmployer());
        revision.setRoleTitle(role.getJobTitle());
        revision.setStartDate(parsePartialDate(role.getStartDate()));
        revision.setEndDate(parsePartialDate(role.getEndDate()));
        revision.setOngoing(role.getStatus() == RoleStatus.CURRENT);
        revision.setResponsibilities(role.getKeyResponsibilities());
        addFact(revision, "ROLE_TITLE", role.getJobTitle());
        addFact(revision, "ORGANISATION", role.getEmployer());
        addFact(revision, "START_DATE", role.getStartDate());
        addFact(revision, "END_DATE",
                role.getStatus() == RoleStatus.CURRENT ? "Present" : role.getEndDate());
        addFact(revision, "RESPONSIBILITY", role.getKeyResponsibilities());
        entry.addRevision(revision);
        return entry;
    }

    private EvidenceEntry qualificationEntry(
            UserProfile profile,
            Qualification qualification,
            String contentDigest) {
        EvidenceEntry entry = baseEntry(profile, EvidenceCategory.QUALIFICATION_TRAINING);
        EvidenceRevision revision = baseRevision(qualification.getQualificationName(), contentDigest);
        revision.setOrganisationContext(qualification.getIssuingBody());
        revision.setQualificationTitle(qualification.getQualificationName());
        revision.setIssuer(qualification.getIssuingBody());
        revision.setResultOrStatus(qualification.getGrade() == null
                ? qualification.getStatus().name()
                : qualification.getStatus().name() + " — " + qualification.getGrade());
        revision.setIssueDate(parsePartialDate(qualification.getDateAchieved()));
        revision.setEndDate(parsePartialDate(qualification.getExpectedCompletion()));
        addFact(revision, "QUALIFICATION_TITLE", qualification.getQualificationName());
        addFact(revision, "ISSUER", qualification.getIssuingBody());
        addFact(revision, "STATUS", qualification.getStatus().name());
        addFact(revision, "RESULT", qualification.getGrade());
        addFact(revision, "ISSUE_DATE", qualification.getDateAchieved());
        addFact(revision, "EXPECTED_COMPLETION", qualification.getExpectedCompletion());
        entry.addRevision(revision);
        return entry;
    }

    private EvidenceEntry baseEntry(UserProfile profile, EvidenceCategory category) {
        EvidenceEntry entry = new EvidenceEntry();
        entry.setUserProfile(profile);
        entry.setCategory(category);
        entry.setVisibility(EvidenceVisibility.VISIBLE);
        entry.setLifecycle(EvidenceLifecycle.ACTIVE);
        entry.setReviewRequired(true);
        return entry;
    }

    private EvidenceRevision baseRevision(String heading, String contentDigest) {
        EvidenceRevision revision = new EvidenceRevision();
        revision.setRevisionNumber(1);
        revision.setConfirmationState(EvidenceConfirmationState.DRAFT);
        revision.setContentDigest(contentDigest);
        revision.setHeading(heading);
        revision.setCreatedBy(EvidenceRevisionCreator.LEGACY_MIGRATION);
        return revision;
    }

    private void addFact(EvidenceRevision revision, String type, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        EvidenceFact fact = new EvidenceFact();
        fact.setFactType(type);
        fact.setFactValue(value);
        fact.setNumericClaim(value.chars().anyMatch(Character::isDigit));
        revision.addFact(fact);
    }

    private String roleSourceHash(Role role) {
        return digestValues(Arrays.asList(
                role.getJobTitle(),
                role.getEmployer(),
                role.getStatus(),
                role.getStartDate(),
                role.getEndDate(),
                role.getKeyResponsibilities()));
    }

    private String qualificationSourceHash(Qualification qualification) {
        return digestValues(Arrays.asList(
                qualification.getQualificationName(),
                qualification.getIssuingBody(),
                qualification.getStatus(),
                qualification.getGrade(),
                qualification.getDateAchieved(),
                qualification.getExpectedCompletion()));
    }

    private String digestValues(List<?> values) {
        StringBuilder canonical = new StringBuilder("legacy-evidence-v1|");
        values.forEach(value -> DigestSupport.append(canonical, value));
        return DigestSupport.sha256(canonical.toString());
    }

    private PartialDate parsePartialDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() == 7) {
            YearMonth parsed = YearMonth.parse(value);
            return new PartialDate(DatePrecision.MONTH, parsed.getYear(), parsed.getMonthValue(), null);
        }
        LocalDate parsed = LocalDate.parse(value);
        return new PartialDate(
                DatePrecision.DAY,
                parsed.getYear(),
                parsed.getMonthValue(),
                parsed.getDayOfMonth());
    }
}
