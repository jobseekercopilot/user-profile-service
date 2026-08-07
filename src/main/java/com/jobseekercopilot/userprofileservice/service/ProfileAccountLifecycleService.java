package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.dto.ProfilePersonalDataExport;
import com.jobseekercopilot.userprofileservice.repository.EvidenceEntryRepository;
import com.jobseekercopilot.userprofileservice.repository.EvidenceSnapshotRepository;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileAccountLifecycleService {

    private final UserProfileRepository profileRepository;
    private final EvidenceEntryRepository evidenceEntryRepository;
    private final EvidenceSnapshotRepository evidenceSnapshotRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ProfilePersonalDataExport export(String ownerId) {
        var profile = profileRepository.findByUserId(ownerId).orElse(null);
        if (profile == null) {
            return new ProfilePersonalDataExport(
                    "profile-personal-data.v1", clock.instant(), null, List.of(), List.of());
        }
        var entries = evidenceEntryRepository
                .findByUserProfileUserIdOrderByUpdatedAtDesc(ownerId);
        var snapshots = evidenceSnapshotRepository
                .findByUserProfileUserIdOrderByCreatedAtAsc(ownerId);
        entries.forEach(entry -> entry.getRevisions().forEach(revision -> {
            revision.getDemonstratedSkills().size();
            revision.getSupportingLinks().size();
            revision.getFacts().size();
        }));
        snapshots.forEach(snapshot -> {
            snapshot.getSectionOrder().size();
            snapshot.getSelections().forEach(selection -> selection.getFacts().size());
        });
        return new ProfilePersonalDataExport(
                "profile-personal-data.v1",
                clock.instant(),
                profile,
                List.copyOf(entries),
                List.copyOf(snapshots));
    }

    @Transactional
    public void erase(String ownerId) {
        profileRepository.deleteByUserId(ownerId);
        profileRepository.flush();
    }
}
