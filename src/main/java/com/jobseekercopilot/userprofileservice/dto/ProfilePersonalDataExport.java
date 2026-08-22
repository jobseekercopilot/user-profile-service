package com.jobseekercopilot.userprofileservice.dto;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshot;
import java.time.Instant;
import java.util.List;

public record ProfilePersonalDataExport(
        String schemaVersion,
        Instant generatedAt,
        UserProfile profile,
        List<EvidenceEntry> evidenceEntries,
        List<EvidenceSnapshot> evidenceSnapshots) {
}
