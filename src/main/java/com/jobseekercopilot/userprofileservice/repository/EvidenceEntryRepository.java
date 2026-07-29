package com.jobseekercopilot.userprofileservice.repository;

import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceEntryRepository extends JpaRepository<EvidenceEntry, Long> {

    boolean existsByUserProfileIdAndMigrationKey(Long userProfileId, String migrationKey);

    List<EvidenceEntry> findByUserProfileUserIdOrderByUpdatedAtDesc(String userId);

    Optional<EvidenceEntry> findByEntryIdAndUserProfileUserId(String entryId, String userId);
}
