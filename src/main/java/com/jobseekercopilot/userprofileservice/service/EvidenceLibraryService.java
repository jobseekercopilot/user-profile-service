package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.exception.ResourceNotFoundException;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.repository.EvidenceEntryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EvidenceLibraryService {

    private final EvidenceEntryRepository evidenceRepository;
    private final LegacyEvidenceMigrator legacyEvidenceMigrator;

    public EvidenceLibraryService(
            EvidenceEntryRepository evidenceRepository,
            LegacyEvidenceMigrator legacyEvidenceMigrator) {
        this.evidenceRepository = evidenceRepository;
        this.legacyEvidenceMigrator = legacyEvidenceMigrator;
    }

    public List<EvidenceEntry> listForOwner(String userId) {
        legacyEvidenceMigrator.migrateForOwner(userId);
        return evidenceRepository.findByUserProfileUserIdOrderByUpdatedAtDesc(userId);
    }

    public EvidenceEntry getForOwner(String userId, String entryId) {
        legacyEvidenceMigrator.migrateForOwner(userId);
        return evidenceRepository.findByEntryIdAndUserProfileUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence entry not found"));
    }
}
