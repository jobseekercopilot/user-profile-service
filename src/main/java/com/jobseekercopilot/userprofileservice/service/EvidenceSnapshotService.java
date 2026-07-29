package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.exception.EvidenceConflictException;
import com.jobseekercopilot.userprofileservice.exception.EvidenceNotFoundException;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceCategory;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceConfirmationState;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceFact;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceLifecycle;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevision;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshot;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshotFact;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshotRequest;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshotSelection;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceVisibility;
import com.jobseekercopilot.userprofileservice.repository.EvidenceEntryRepository;
import com.jobseekercopilot.userprofileservice.repository.EvidenceSnapshotRepository;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceSnapshotService {

    private static final int MAX_SNAPSHOT_FACTS = 50;

    private final EvidenceSnapshotRepository snapshotRepository;
    private final EvidenceEntryRepository evidenceRepository;
    private final UserProfileRepository profileRepository;
    private final LegacyEvidenceMigrator legacyEvidenceMigrator;

    public EvidenceSnapshotService(
            EvidenceSnapshotRepository snapshotRepository,
            EvidenceEntryRepository evidenceRepository,
            UserProfileRepository profileRepository,
            LegacyEvidenceMigrator legacyEvidenceMigrator) {
        this.snapshotRepository = snapshotRepository;
        this.evidenceRepository = evidenceRepository;
        this.profileRepository = profileRepository;
        this.legacyEvidenceMigrator = legacyEvidenceMigrator;
    }

    @Transactional
    public EvidenceSnapshot create(String userId, EvidenceSnapshotRequest request) {
        validateRequest(request);
        legacyEvidenceMigrator.migrateForOwner(userId);
        var profile = profileRepository.findByUserId(userId)
                .orElseThrow(EvidenceNotFoundException::new);
        if (profile.getRevisionId() == null || profile.getContentDigest() == null) {
            throw new EvidenceConflictException("Profile revision metadata is unavailable");
        }

        EvidenceSnapshot snapshot = new EvidenceSnapshot();
        snapshot.setUserProfile(profile);
        snapshot.setPurpose(request.getPurpose());
        snapshot.setProfileRevisionId(profile.getRevisionId());
        snapshot.setProfileContentDigest(profile.getContentDigest());
        snapshot.setSectionOrder(List.copyOf(request.getSectionOrder()));

        int factCount = 0;
        for (String entryId : request.getEntryIds()) {
            EvidenceEntry entry = evidenceRepository
                    .findLockedByEntryIdAndUserProfileUserId(entryId, userId)
                    .orElseThrow(EvidenceNotFoundException::new);
            if (!request.getSectionOrder().contains(entry.getCategory())) {
                throw new EvidenceConflictException("Selected evidence has no requested section");
            }
            EvidenceRevision revision = eligibleRevision(entry);
            factCount += revision.getFacts().size();
            if (factCount > MAX_SNAPSHOT_FACTS) {
                throw new EvidenceConflictException(
                        "Selected evidence exceeds the snapshot fact limit");
            }
            snapshot.addSelection(selection(entry, revision));
        }

        snapshot.setSnapshotDigest(digest(snapshot));
        return snapshotRepository.saveAndFlush(snapshot);
    }

    @Transactional(readOnly = true)
    public EvidenceSnapshot getForOwner(String userId, String snapshotId) {
        return snapshotRepository.findBySnapshotIdAndUserProfileUserId(snapshotId, userId)
                .orElseThrow(EvidenceNotFoundException::new);
    }

    private void validateRequest(EvidenceSnapshotRequest request) {
        if (request.getPurpose() == null
                || request.getEntryIds() == null
                || request.getEntryIds().isEmpty()
                || request.getEntryIds().size() > 50
                || request.getSectionOrder() == null
                || request.getSectionOrder().isEmpty()) {
            throw new IllegalArgumentException("Snapshot selection is invalid");
        }
        Set<String> entryIds = new HashSet<>(request.getEntryIds());
        Set<EvidenceCategory> sectionOrder = new HashSet<>(request.getSectionOrder());
        if (entryIds.size() != request.getEntryIds().size()
                || sectionOrder.size() != request.getSectionOrder().size()) {
            throw new EvidenceConflictException("Snapshot selection contains duplicates");
        }
    }

    private EvidenceRevision eligibleRevision(EvidenceEntry entry) {
        if (entry.getLifecycle() != EvidenceLifecycle.ACTIVE
                || entry.getVisibility() != EvidenceVisibility.VISIBLE
                || entry.isReviewRequired()) {
            throw new EvidenceConflictException("Evidence is not eligible for a new snapshot");
        }
        EvidenceRevision latest = entry.getRevisions().stream()
                .max(java.util.Comparator.comparingInt(EvidenceRevision::getRevisionNumber))
                .orElseThrow(() -> new EvidenceConflictException("Evidence has no revision"));
        if (latest.getConfirmationState() != EvidenceConfirmationState.USER_CONFIRMED) {
            throw new EvidenceConflictException("The latest evidence revision is not confirmed");
        }
        return latest;
    }

    private EvidenceSnapshotSelection selection(EvidenceEntry entry, EvidenceRevision revision) {
        EvidenceSnapshotSelection selection = new EvidenceSnapshotSelection();
        selection.setEntryId(entry.getEntryId());
        selection.setRevisionId(revision.getRevisionId());
        selection.setRevisionNumber(revision.getRevisionNumber());
        selection.setCategory(entry.getCategory());
        selection.setContentDigest(revision.getContentDigest());
        for (EvidenceFact source : revision.getFacts()) {
            EvidenceSnapshotFact fact = new EvidenceSnapshotFact();
            fact.setFactId(source.getFactId());
            fact.setFactType(source.getFactType());
            fact.setFactValue(source.getFactValue());
            fact.setNumericClaim(source.isNumericClaim());
            selection.addFact(fact);
        }
        return selection;
    }

    private String digest(EvidenceSnapshot snapshot) {
        StringBuilder canonical = new StringBuilder("evidence-snapshot-v1|");
        DigestSupport.append(canonical, snapshot.getPurpose());
        DigestSupport.append(canonical, snapshot.getProfileRevisionId());
        DigestSupport.append(canonical, snapshot.getProfileContentDigest());
        snapshot.getSectionOrder().forEach(category -> DigestSupport.append(canonical, category));
        for (EvidenceSnapshotSelection selection : snapshot.getSelections()) {
            DigestSupport.append(canonical, selection.getEntryId());
            DigestSupport.append(canonical, selection.getRevisionId());
            DigestSupport.append(canonical, selection.getRevisionNumber());
            DigestSupport.append(canonical, selection.getCategory());
            DigestSupport.append(canonical, selection.getContentDigest());
            for (EvidenceSnapshotFact fact : selection.getFacts()) {
                DigestSupport.append(canonical, fact.getFactId());
                DigestSupport.append(canonical, fact.getFactType());
                DigestSupport.append(canonical, fact.getFactValue());
                DigestSupport.append(canonical, fact.isNumericClaim());
            }
        }
        return DigestSupport.sha256(canonical.toString());
    }
}
