package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.Qualification;
import com.jobseekercopilot.userprofileservice.model.Role;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import com.jobseekercopilot.userprofileservice.validation.QualificationValidator;
import com.jobseekercopilot.userprofileservice.validation.RoleValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;
    private final QualificationValidator qualificationValidator;
    private final RoleValidator roleValidator;

    public UserProfileService(UserProfileRepository userProfileRepository, QualificationValidator qualificationValidator, RoleValidator roleValidator) {
        this.userProfileRepository = userProfileRepository;
        this.qualificationValidator = qualificationValidator;
        this.roleValidator = roleValidator;
    }

    public Optional<UserProfile> getProfileByUserId(String userId) {
        long startedAt = System.nanoTime();
        Optional<UserProfile> profile = userProfileRepository.findByUserId(userId);
        log.info("User profile lookup userId={} found={} durationMs={}",
                userId,
                profile.isPresent(),
                (System.nanoTime() - startedAt) / 1_000_000);
        return profile;
    }

    public UserProfile createOrUpdateProfile(String userId, UserProfile userProfile) {
        long startedAt = System.nanoTime();
        log.info("User profile save started userId={} skillsCount={} qualificationsCount={} rolesCount={} hasAspirations={} hasWorkPreferences={}",
                userId,
                userProfile == null || userProfile.getSkills() == null ? 0 : userProfile.getSkills().size(),
                userProfile == null || userProfile.getQualifications() == null ? 0 : userProfile.getQualifications().size(),
                userProfile == null || userProfile.getRoles() == null ? 0 : userProfile.getRoles().size(),
                userProfile != null && userProfile.getAspirations() != null,
                userProfile != null && userProfile.getWorkPreferences() != null);
        if (userProfile == null) {
            throw new IllegalArgumentException("User profile cannot be null");
        }
        userProfile.setUserId(userId);

        if (userProfile.getQualifications() != null) {
            for (Qualification qualification : userProfile.getQualifications()) {
                qualificationValidator.validate(qualification);
            }
        }

        if (userProfile.getRoles() != null) {
            for (Role role : userProfile.getRoles()) {
                roleValidator.validate(role);
            }
        }

        return userProfileRepository.findByUserId(userId)
                .map(existingProfile -> {
                    existingProfile.setSkills(userProfile.getSkills());
                    existingProfile.setAspirations(userProfile.getAspirations());
                    existingProfile.setWorkPreferences(userProfile.getWorkPreferences());
                    replaceQualifications(existingProfile, userProfile.getQualifications());
                    replaceRoles(existingProfile, userProfile.getRoles());
                    UserProfile saved = userProfileRepository.save(existingProfile);
                    log.info("User profile updated userId={} profileId={} durationMs={}",
                            userId,
                            saved.getId(),
                            (System.nanoTime() - startedAt) / 1_000_000);
                    return saved;
                })
                .orElseGet(() -> {
                    replaceQualifications(userProfile, userProfile.getQualifications());
                    replaceRoles(userProfile, userProfile.getRoles());
                    UserProfile saved = userProfileRepository.save(userProfile);
                    log.info("User profile created userId={} profileId={} durationMs={}",
                            userId,
                            saved.getId(),
                            (System.nanoTime() - startedAt) / 1_000_000);
                    return saved;
                });
    }

    private void replaceQualifications(UserProfile profile, List<Qualification> qualifications) {
        List<Qualification> replacements = qualifications == null
                ? List.of()
                : new ArrayList<>(qualifications);

        if (profile.getQualifications() == null) {
            profile.setQualifications(new ArrayList<>());
        }

        profile.getQualifications().clear();
        for (Qualification qualification : replacements) {
            qualification.setId(null);
            qualification.setUserProfile(profile);
            profile.getQualifications().add(qualification);
        }
    }

    private void replaceRoles(UserProfile profile, List<Role> roles) {
        List<Role> replacements = roles == null
                ? List.of()
                : new ArrayList<>(roles);

        if (profile.getRoles() == null) {
            profile.setRoles(new ArrayList<>());
        }

        profile.getRoles().clear();
        for (Role role : replacements) {
            role.setId(null);
            role.setUserProfile(profile);
            profile.getRoles().add(role);
        }
    }
}
