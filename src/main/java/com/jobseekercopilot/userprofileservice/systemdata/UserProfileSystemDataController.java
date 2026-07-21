package com.jobseekercopilot.userprofileservice.systemdata;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import com.jobseekercopilot.userprofileservice.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile(EnvironmentDataGuard.PROFILE)
@RequestMapping("/internal/system-data")
public class UserProfileSystemDataController {
    private final EnvironmentDataGuard guard;
    private final UserProfileService profileService;
    private final UserProfileRepository repository;

    public UserProfileSystemDataController(EnvironmentDataGuard guard, UserProfileService profileService, UserProfileRepository repository) {
        this.guard = guard;
        this.profileService = profileService;
        this.repository = repository;
    }

    @PostMapping("/seed/profiles/{userId}")
    public ResponseEntity<SystemDataResult> seedProfile(@PathVariable String userId, @RequestBody UserProfile profile) {
        guard.requireEnabled();
        UserProfile saved = profileService.createOrUpdateProfile(userId, profile);
        return ResponseEntity.ok(SystemDataResult.success("SEED", 1, guard.activeEnvironment(), Map.of(
                "userId", userId,
                "profileId", saved.getId())));
    }

    @Transactional
    @DeleteMapping("/scenario/{scenarioId}/profiles/{userId}")
    public ResponseEntity<SystemDataResult> resetProfile(@PathVariable String scenarioId, @PathVariable String userId) {
        guard.requireEnabled();
        boolean existed = repository.findByUserId(userId).isPresent();
        if (existed) {
            repository.deleteByUserId(userId);
        }
        return ResponseEntity.ok(SystemDataResult.success("RESET", existed ? 1 : 0, guard.activeEnvironment(), Map.of(
                "scenarioId", scenarioId,
                "userId", userId)));
    }

    @GetMapping("/verify/profiles/{userId}")
    public ResponseEntity<SystemDataResult> verifyProfile(@PathVariable String userId) {
        guard.requireEnabled();
        return ResponseEntity.ok(SystemDataResult.success("VERIFY", repository.findByUserId(userId).isPresent() ? 1 : 0, guard.activeEnvironment(), Map.of(
                "userId", userId,
                "exists", repository.findByUserId(userId).isPresent())));
    }
}
