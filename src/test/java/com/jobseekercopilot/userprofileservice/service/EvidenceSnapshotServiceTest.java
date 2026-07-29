package com.jobseekercopilot.userprofileservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobseekercopilot.userprofileservice.exception.EvidenceConflictException;
import com.jobseekercopilot.userprofileservice.exception.EvidenceNotFoundException;
import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceCategory;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceConfirmationState;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceFact;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceLifecycle;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevision;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshot;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshotPurpose;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshotRequest;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceVisibility;
import com.jobseekercopilot.userprofileservice.repository.EvidenceEntryRepository;
import com.jobseekercopilot.userprofileservice.repository.EvidenceSnapshotRepository;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvidenceSnapshotServiceTest {

    private static final String OWNER = "owner-1";
    private static final String ENTRY_ID = "10000000-0000-0000-0000-000000000001";
    private static final String REVISION_ID = "20000000-0000-0000-0000-000000000001";
    private static final String FACT_ID = "30000000-0000-0000-0000-000000000001";

    @Mock
    private EvidenceSnapshotRepository snapshotRepository;

    @Mock
    private EvidenceEntryRepository evidenceRepository;

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private LegacyEvidenceMigrator legacyEvidenceMigrator;

    private EvidenceSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new EvidenceSnapshotService(
                snapshotRepository, evidenceRepository, profileRepository, legacyEvidenceMigrator);
    }

    @Test
    void createsPurposeBoundCopyUsingStableConfirmedFactIds() {
        UserProfile profile = profile();
        EvidenceEntry entry = eligibleEntry();
        when(profileRepository.findByUserId(OWNER)).thenReturn(Optional.of(profile));
        when(evidenceRepository.findLockedByEntryIdAndUserProfileUserId(ENTRY_ID, OWNER))
                .thenReturn(Optional.of(entry));
        when(snapshotRepository.saveAndFlush(any(EvidenceSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceSnapshot result = service.create(OWNER, request(ENTRY_ID));

        assertEquals(EvidenceSnapshotPurpose.CV, result.getPurpose());
        assertEquals(profile.getRevisionId(), result.getProfileRevisionId());
        assertEquals(List.of(EvidenceCategory.PROJECT), result.getSectionOrder());
        assertEquals(ENTRY_ID, result.getSelections().get(0).getEntryId());
        assertEquals(REVISION_ID, result.getSelections().get(0).getRevisionId());
        assertEquals(FACT_ID, result.getSelections().get(0).getFacts().get(0).getFactId());
        assertEquals("Built an accessible search prototype",
                result.getSelections().get(0).getFacts().get(0).getFactValue());
        assertEquals(64, result.getSnapshotDigest().length());
        assertNotSame(entry.getRevisions().get(0).getFacts().get(0),
                result.getSelections().get(0).getFacts().get(0));
        verify(legacyEvidenceMigrator).migrateForOwner(OWNER);
    }

    @Test
    void rejectsDuplicateBrowserSelectionsBeforeReadingEvidence() {
        EvidenceSnapshotRequest request = request(ENTRY_ID);
        request.setEntryIds(List.of(ENTRY_ID, ENTRY_ID));

        assertThrows(EvidenceConflictException.class, () -> service.create(OWNER, request));

        verify(evidenceRepository, never())
                .findLockedByEntryIdAndUserProfileUserId(any(), any());
    }

    @Test
    void rejectsCrossOwnerSelectionWithoutDisclosingItExists() {
        when(profileRepository.findByUserId(OWNER)).thenReturn(Optional.of(profile()));
        when(evidenceRepository.findLockedByEntryIdAndUserProfileUserId(ENTRY_ID, OWNER))
                .thenReturn(Optional.empty());

        assertThrows(EvidenceNotFoundException.class, () -> service.create(OWNER, request(ENTRY_ID)));
    }

    @Test
    void rejectsLatestDraftRatherThanFallingBackToStaleConfirmedRevision() {
        EvidenceEntry entry = eligibleEntry();
        EvidenceRevision draft = revision(2, EvidenceConfirmationState.DRAFT);
        draft.setRevisionId("20000000-0000-0000-0000-000000000002");
        entry.addRevision(draft);
        stubOwned(entry);

        assertThrows(EvidenceConflictException.class, () -> service.create(OWNER, request(ENTRY_ID)));
        verify(snapshotRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsArchivedSupersededHiddenAndReviewRequiredEvidence() {
        for (EvidenceEntry ineligible : List.of(
                entry(EvidenceLifecycle.ARCHIVED, EvidenceVisibility.VISIBLE, false),
                entry(EvidenceLifecycle.SUPERSEDED, EvidenceVisibility.VISIBLE, false),
                entry(EvidenceLifecycle.ACTIVE, EvidenceVisibility.HIDDEN, false),
                entry(EvidenceLifecycle.ACTIVE, EvidenceVisibility.VISIBLE, true))) {
            stubOwned(ineligible);
            assertThrows(EvidenceConflictException.class,
                    () -> service.create(OWNER, request(ENTRY_ID)));
        }
    }

    @Test
    void requiresEverySelectedCategoryInSafeSectionOrder() {
        EvidenceSnapshotRequest request = request(ENTRY_ID);
        request.setSectionOrder(List.of(EvidenceCategory.EMPLOYMENT));
        stubOwned(eligibleEntry());

        assertThrows(EvidenceConflictException.class, () -> service.create(OWNER, request));
    }

    @Test
    void retrievesSnapshotsOnlyThroughOwnerScopedLookup() {
        EvidenceSnapshot snapshot = new EvidenceSnapshot();
        when(snapshotRepository.findBySnapshotIdAndUserProfileUserId("snapshot-1", OWNER))
                .thenReturn(Optional.of(snapshot));

        assertEquals(snapshot, service.getForOwner(OWNER, "snapshot-1"));
        assertThrows(EvidenceNotFoundException.class,
                () -> service.getForOwner("other-owner", "snapshot-1"));
    }

    private void stubOwned(EvidenceEntry entry) {
        when(profileRepository.findByUserId(OWNER)).thenReturn(Optional.of(profile()));
        when(evidenceRepository.findLockedByEntryIdAndUserProfileUserId(ENTRY_ID, OWNER))
                .thenReturn(Optional.of(entry));
    }

    private EvidenceSnapshotRequest request(String... entryIds) {
        EvidenceSnapshotRequest request = new EvidenceSnapshotRequest();
        request.setPurpose(EvidenceSnapshotPurpose.CV);
        request.setEntryIds(List.of(entryIds));
        request.setSectionOrder(List.of(EvidenceCategory.PROJECT));
        return request;
    }

    private UserProfile profile() {
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setUserId(OWNER);
        profile.setRevisionId("40000000-0000-0000-0000-000000000001");
        profile.setContentDigest("a".repeat(64));
        return profile;
    }

    private EvidenceEntry eligibleEntry() {
        return entry(EvidenceLifecycle.ACTIVE, EvidenceVisibility.VISIBLE, false);
    }

    private EvidenceEntry entry(
            EvidenceLifecycle lifecycle,
            EvidenceVisibility visibility,
            boolean reviewRequired) {
        EvidenceEntry entry = new EvidenceEntry();
        entry.setEntryId(ENTRY_ID);
        entry.setCategory(EvidenceCategory.PROJECT);
        entry.setLifecycle(lifecycle);
        entry.setVisibility(visibility);
        entry.setReviewRequired(reviewRequired);
        entry.setRevisions(new ArrayList<>());
        entry.addRevision(revision(1, EvidenceConfirmationState.USER_CONFIRMED));
        return entry;
    }

    private EvidenceRevision revision(int number, EvidenceConfirmationState state) {
        EvidenceRevision revision = new EvidenceRevision();
        revision.setRevisionId(REVISION_ID);
        revision.setRevisionNumber(number);
        revision.setConfirmationState(state);
        revision.setContentDigest("b".repeat(64));
        EvidenceFact fact = new EvidenceFact();
        fact.setFactId(FACT_ID);
        fact.setFactType("ACHIEVEMENTS");
        fact.setFactValue("Built an accessible search prototype");
        fact.setNumericClaim(false);
        revision.addFact(fact);
        return revision;
    }
}
