package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.exception.EvidenceConflictException;
import com.jobseekercopilot.userprofileservice.exception.EvidenceNotFoundException;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceConfirmationState;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceLifecycle;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevision;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceRevisionCreator;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceVisibility;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceWriteRequest;
import com.jobseekercopilot.userprofileservice.repository.EvidenceEntryRepository;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceLibraryService {

    private final EvidenceEntryRepository evidenceRepository;
    private final UserProfileRepository profileRepository;
    private final LegacyEvidenceMigrator legacyEvidenceMigrator;
    private final EvidenceRequestProcessor requestProcessor;

    public EvidenceLibraryService(
            EvidenceEntryRepository evidenceRepository,
            UserProfileRepository profileRepository,
            LegacyEvidenceMigrator legacyEvidenceMigrator,
            EvidenceRequestProcessor requestProcessor) {
        this.evidenceRepository = evidenceRepository;
        this.profileRepository = profileRepository;
        this.legacyEvidenceMigrator = legacyEvidenceMigrator;
        this.requestProcessor = requestProcessor;
    }

    public List<EvidenceEntry> listForOwner(String userId) {
        return listForOwner(userId, false);
    }

    @Transactional
    public List<EvidenceEntry> listForOwner(String userId, boolean includeArchived) {
        legacyEvidenceMigrator.migrateForOwner(userId);
        return includeArchived
                ? evidenceRepository.findByUserProfileUserIdOrderByUpdatedAtDesc(userId)
                : evidenceRepository.findByUserProfileUserIdAndLifecycleNotOrderByUpdatedAtDesc(
                        userId, EvidenceLifecycle.ARCHIVED);
    }

    @Transactional
    public EvidenceEntry getForOwner(String userId, String entryId) {
        legacyEvidenceMigrator.migrateForOwner(userId);
        return owned(userId, entryId);
    }

    @Transactional
    public EvidenceEntry create(String userId, EvidenceWriteRequest request) {
        var profile = profileRepository.findByUserId(userId)
                .orElseThrow(EvidenceNotFoundException::new);
        EvidenceEntry entry = new EvidenceEntry();
        entry.setUserProfile(profile);
        entry.setCategory(request.getCategory());
        entry.setVisibility(EvidenceVisibility.VISIBLE);
        entry.setLifecycle(EvidenceLifecycle.ACTIVE);
        entry.setReviewRequired(false);
        entry.addRevision(requestProcessor.revision(
                request, 1, EvidenceConfirmationState.DRAFT, EvidenceRevisionCreator.USER));
        return evidenceRepository.saveAndFlush(entry);
    }

    @Transactional
    public EvidenceEntry edit(
            String userId,
            String entryId,
            Long expectedVersion,
            EvidenceWriteRequest request) {
        EvidenceEntry entry = owned(userId, entryId);
        checkVersion(entry, expectedVersion);
        requireLifecycle(entry, EvidenceLifecycle.ACTIVE);
        if (request.getCategory() != entry.getCategory()) {
            throw new EvidenceConflictException("Evidence category cannot change");
        }
        int revisionNumber = latest(entry).getRevisionNumber() + 1;
        entry.addRevision(requestProcessor.revision(
                request,
                revisionNumber,
                EvidenceConfirmationState.DRAFT,
                EvidenceRevisionCreator.USER));
        entry.setReviewRequired(false);
        touch(entry);
        return evidenceRepository.saveAndFlush(entry);
    }

    @Transactional
    public EvidenceEntry confirm(String userId, String entryId, Long expectedVersion) {
        EvidenceEntry entry = owned(userId, entryId);
        checkVersion(entry, expectedVersion);
        requireLifecycle(entry, EvidenceLifecycle.ACTIVE);
        EvidenceRevision latest = latest(entry);
        if (latest.getConfirmationState() != EvidenceConfirmationState.DRAFT) {
            throw new EvidenceConflictException("Only the latest draft can be confirmed");
        }
        EvidenceWriteRequest request = requestProcessor.fromRevision(entry, latest);
        entry.addRevision(requestProcessor.revision(
                request,
                latest.getRevisionNumber() + 1,
                EvidenceConfirmationState.USER_CONFIRMED,
                EvidenceRevisionCreator.USER));
        entry.setReviewRequired(false);
        touch(entry);
        return evidenceRepository.saveAndFlush(entry);
    }

    @Transactional
    public EvidenceEntry setVisibility(
            String userId,
            String entryId,
            Long expectedVersion,
            EvidenceVisibility visibility) {
        EvidenceEntry entry = owned(userId, entryId);
        checkVersion(entry, expectedVersion);
        entry.setVisibility(visibility);
        touch(entry);
        return evidenceRepository.saveAndFlush(entry);
    }

    @Transactional
    public EvidenceEntry archive(String userId, String entryId, Long expectedVersion) {
        EvidenceEntry entry = owned(userId, entryId);
        checkVersion(entry, expectedVersion);
        requireLifecycle(entry, EvidenceLifecycle.ACTIVE);
        entry.setLifecycle(EvidenceLifecycle.ARCHIVED);
        touch(entry);
        return evidenceRepository.saveAndFlush(entry);
    }

    @Transactional
    public EvidenceEntry restore(String userId, String entryId, Long expectedVersion) {
        EvidenceEntry entry = owned(userId, entryId);
        checkVersion(entry, expectedVersion);
        requireLifecycle(entry, EvidenceLifecycle.ARCHIVED);
        entry.setLifecycle(EvidenceLifecycle.ACTIVE);
        touch(entry);
        return evidenceRepository.saveAndFlush(entry);
    }

    @Transactional
    public EvidenceEntry supersede(
            String userId,
            String entryId,
            Long expectedVersion,
            String replacementEntryId) {
        EvidenceEntry entry = owned(userId, entryId);
        EvidenceEntry replacement = owned(userId, replacementEntryId);
        checkVersion(entry, expectedVersion);
        requireLifecycle(entry, EvidenceLifecycle.ACTIVE);
        requireLifecycle(replacement, EvidenceLifecycle.ACTIVE);
        if (entry.getEntryId().equals(replacement.getEntryId())) {
            throw new EvidenceConflictException("Evidence cannot supersede itself");
        }
        entry.setLifecycle(EvidenceLifecycle.SUPERSEDED);
        entry.setSupersededByEntryId(replacement.getEntryId());
        touch(entry);
        return evidenceRepository.saveAndFlush(entry);
    }

    public EvidenceRevision latest(EvidenceEntry entry) {
        return entry.getRevisions().stream()
                .max(java.util.Comparator.comparingInt(EvidenceRevision::getRevisionNumber))
                .orElseThrow(() -> new EvidenceConflictException("Evidence has no revision"));
    }

    private EvidenceEntry owned(String userId, String entryId) {
        return evidenceRepository.findByEntryIdAndUserProfileUserId(entryId, userId)
                .orElseThrow(EvidenceNotFoundException::new);
    }

    private void checkVersion(EvidenceEntry entry, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(entry.getVersion())) {
            throw new EvidenceConflictException("Evidence changed; reload it before saving");
        }
    }

    private void requireLifecycle(EvidenceEntry entry, EvidenceLifecycle lifecycle) {
        if (entry.getLifecycle() != lifecycle) {
            throw new EvidenceConflictException("Evidence lifecycle does not allow this action");
        }
    }

    private void touch(EvidenceEntry entry) {
        entry.setUpdatedAt(Instant.now());
    }
}
