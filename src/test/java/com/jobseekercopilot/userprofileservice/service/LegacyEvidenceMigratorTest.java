package com.jobseekercopilot.userprofileservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobseekercopilot.userprofileservice.model.Qualification;
import com.jobseekercopilot.userprofileservice.model.QualificationStatus;
import com.jobseekercopilot.userprofileservice.model.Role;
import com.jobseekercopilot.userprofileservice.model.RoleStatus;
import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceCategory;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceConfirmationState;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceLifecycle;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevisionCreator;
import com.jobseekercopilot.userprofileservice.repository.EvidenceEntryRepository;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegacyEvidenceMigratorTest {

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private EvidenceEntryRepository evidenceRepository;

    private final ProfileDigestCalculator profileDigestCalculator = new ProfileDigestCalculator();

    @Test
    void migratesLegacyRowsAsReviewRequiredDraftsWithoutInventingContent() {
        UserProfile profile = profile();
        when(evidenceRepository.existsByUserProfileIdAndMigrationKey(anyLong(), anyString()))
                .thenReturn(false);
        when(evidenceRepository.saveAndFlush(any(EvidenceEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        LegacyEvidenceMigrator migrator = new LegacyEvidenceMigrator(
                profileRepository,
                evidenceRepository,
                profileDigestCalculator);

        assertEquals(2, migrator.migrateProfile(profile));

        ArgumentCaptor<EvidenceEntry> entries = ArgumentCaptor.forClass(EvidenceEntry.class);
        verify(evidenceRepository, times(2)).saveAndFlush(entries.capture());
        EvidenceEntry employment = entries.getAllValues().stream()
                .filter(entry -> entry.getCategory() == EvidenceCategory.EMPLOYMENT)
                .findFirst()
                .orElseThrow();
        assertEquals(EvidenceLifecycle.ACTIVE, employment.getLifecycle());
        assertTrue(employment.isReviewRequired());
        assertNotNull(employment.getSourceHash());
        assertEquals(64, employment.getSourceHash().length());
        assertEquals(EvidenceConfirmationState.DRAFT,
                employment.getRevisions().get(0).getConfirmationState());
        assertEquals(EvidenceRevisionCreator.LEGACY_MIGRATION,
                employment.getRevisions().get(0).getCreatedBy());
        assertEquals("Engineer", employment.getRevisions().get(0).getRoleTitle());
        assertEquals("Built accessible services",
                employment.getRevisions().get(0).getResponsibilities());
        assertFalse(employment.getRevisions().get(0).getFacts().isEmpty());

        EvidenceEntry qualification = entries.getAllValues().stream()
                .filter(entry -> entry.getCategory() == EvidenceCategory.QUALIFICATION_TRAINING)
                .findFirst()
                .orElseThrow();
        assertEquals("Cloud certificate",
                qualification.getRevisions().get(0).getQualificationTitle());
        assertEquals("Training provider", qualification.getRevisions().get(0).getIssuer());
    }

    @Test
    void rerunUsesOwnerScopedMigrationKeyAndCreatesNoDuplicate() {
        UserProfile profile = profile();
        when(evidenceRepository.existsByUserProfileIdAndMigrationKey(anyLong(), anyString()))
                .thenReturn(true);
        LegacyEvidenceMigrator migrator = new LegacyEvidenceMigrator(
                profileRepository,
                evidenceRepository,
                profileDigestCalculator);

        assertEquals(0, migrator.migrateProfile(profile));
        verify(evidenceRepository, times(2))
                .existsByUserProfileIdAndMigrationKey(anyLong(), anyString());
    }

    private UserProfile profile() {
        UserProfile profile = new UserProfile();
        profile.setId(7L);
        profile.setUserId("owner-7");

        Role role = new Role();
        role.setJobTitle("Engineer");
        role.setEmployer("Employer");
        role.setStatus(RoleStatus.CURRENT);
        role.setStartDate("2025-01");
        role.setKeyResponsibilities("Built accessible services");
        profile.setRoles(List.of(role));

        Qualification qualification = new Qualification();
        qualification.setQualificationName("Cloud certificate");
        qualification.setIssuingBody("Training provider");
        qualification.setStatus(QualificationStatus.COMPLETED);
        qualification.setDateAchieved("2025-06");
        profile.setQualifications(List.of(qualification));
        return profile;
    }
}
