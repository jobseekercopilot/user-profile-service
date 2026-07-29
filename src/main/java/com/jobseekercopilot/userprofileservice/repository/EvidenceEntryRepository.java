package com.jobseekercopilot.userprofileservice.repository;

import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceLifecycle;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EvidenceEntryRepository extends JpaRepository<EvidenceEntry, Long> {

    boolean existsByUserProfileIdAndMigrationKey(Long userProfileId, String migrationKey);

    List<EvidenceEntry> findByUserProfileUserIdOrderByUpdatedAtDesc(String userId);

    List<EvidenceEntry> findByUserProfileUserIdAndLifecycleNotOrderByUpdatedAtDesc(
            String userId,
            EvidenceLifecycle lifecycle);

    Optional<EvidenceEntry> findByEntryIdAndUserProfileUserId(String entryId, String userId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<EvidenceEntry> findLockedByEntryIdAndUserProfileUserId(String entryId, String userId);
}
