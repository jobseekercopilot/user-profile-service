package com.jobseekercopilot.userprofileservice.repository;

import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceSnapshotRepository extends JpaRepository<EvidenceSnapshot, Long> {

    Optional<EvidenceSnapshot> findBySnapshotIdAndUserProfileUserId(String snapshotId, String userId);

    List<EvidenceSnapshot> findByUserProfileUserIdOrderByCreatedAtAsc(String userId);
}
