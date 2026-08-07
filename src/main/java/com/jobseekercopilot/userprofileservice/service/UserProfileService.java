package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.ProfilePreferencesUpdate;
import com.jobseekercopilot.userprofileservice.model.Qualification;
import com.jobseekercopilot.userprofileservice.model.Role;
import com.jobseekercopilot.userprofileservice.exception.ProfileValidationException;
import com.jobseekercopilot.userprofileservice.exception.ProfileWriteConflictException;
import com.jobseekercopilot.userprofileservice.exception.ProfileRevisionConflictException;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import com.jobseekercopilot.userprofileservice.validation.ProfileNormalizer;
import com.jobseekercopilot.userprofileservice.validation.QualificationValidator;
import com.jobseekercopilot.userprofileservice.validation.RoleValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;
    private final QualificationValidator qualificationValidator;
    private final RoleValidator roleValidator;
    private final ProfileNormalizer profileNormalizer;
    private final ProfileWriteCoordinator profileWriteCoordinator;
    private final ProfileDigestCalculator profileDigestCalculator;
    private final LegacyEvidenceMigrator legacyEvidenceMigrator;

    public UserProfileService(
            UserProfileRepository userProfileRepository,
            QualificationValidator qualificationValidator,
            RoleValidator roleValidator,
            ProfileNormalizer profileNormalizer,
            ProfileWriteCoordinator profileWriteCoordinator,
            ProfileDigestCalculator profileDigestCalculator,
            LegacyEvidenceMigrator legacyEvidenceMigrator) {
        this.userProfileRepository = userProfileRepository;
        this.qualificationValidator = qualificationValidator;
        this.roleValidator = roleValidator;
        this.profileNormalizer = profileNormalizer;
        this.profileWriteCoordinator = profileWriteCoordinator;
        this.profileDigestCalculator = profileDigestCalculator;
        this.legacyEvidenceMigrator = legacyEvidenceMigrator;
    }

    public Optional<UserProfile> getProfileByUserId(String userId) {
        long startedAt = System.nanoTime();
        legacyEvidenceMigrator.migrateForOwner(userId);
        Optional<UserProfile> profile = userProfileRepository.findByUserId(userId);
        log.info("User profile lookup found={} durationMs={}",
                profile.isPresent(),
                (System.nanoTime() - startedAt) / 1_000_000);
        return profile;
    }

    public UserProfile createOrUpdateProfile(String userId, UserProfile userProfile) {
        return createOrUpdateProfile(userId, userProfile, null);
    }

    public UserProfile createOrUpdateProfile(
            String userId,
            UserProfile userProfile,
            Long expectedRevision) {
        long startedAt = System.nanoTime();
        log.info("User profile save started skillsCount={} qualificationsCount={} rolesCount={} hasAspirations={} hasWorkPreferences={}",
                userProfile == null || userProfile.getSkills() == null ? 0 : userProfile.getSkills().size(),
                userProfile == null || userProfile.getQualifications() == null ? 0 : userProfile.getQualifications().size(),
                userProfile == null || userProfile.getRoles() == null ? 0 : userProfile.getRoles().size(),
                userProfile != null && userProfile.getAspirations() != null,
                userProfile != null && userProfile.getWorkPreferences() != null);
        if (userProfile == null) {
            throw new ProfileValidationException("profile", "NOT_NULL", "User profile cannot be null");
        }
        userProfile.setUserId(userId);
        profileNormalizer.normalize(userProfile);

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

        try {
            UserProfile saved = profileWriteCoordinator.execute(
                    userId,
                    () -> saveProfile(userId, userProfile, expectedRevision, startedAt));
            legacyEvidenceMigrator.migrateForOwner(userId);
            return saved;
        } catch (DataIntegrityViolationException exception) {
            log.warn("User profile write conflict durationMs={}",
                    (System.nanoTime() - startedAt) / 1_000_000);
            throw new ProfileWriteConflictException(exception);
        }
    }

    public UserProfile updatePreferences(
            String userId,
            ProfilePreferencesUpdate update,
            Long expectedRevision) {
        if (update == null) {
            throw new ProfileValidationException(
                    "preferences", "NOT_NULL", "Profile preferences cannot be null");
        }
        boolean replaceSkills = update.getSkills() != null;
        UserProfile normalized = new UserProfile();
        normalized.setSkills(update.getSkills());
        normalized.setAspirations(update.getAspirations());
        normalized.setWorkPreferences(update.getWorkPreferences());
        profileNormalizer.normalize(normalized);
        return profileWriteCoordinator.execute(userId, () -> profileRepositoryUpdatePreferences(
                userId, normalized, replaceSkills, expectedRevision));
    }

    private UserProfile profileRepositoryUpdatePreferences(
            String userId,
            UserProfile preferences,
            boolean replaceSkills,
            Long expectedRevision) {
        return userProfileRepository.findByUserId(userId)
                .map(existing -> {
                    if (expectedRevision != null && !expectedRevision.equals(existing.getRevision())) {
                        throw new ProfileRevisionConflictException();
                    }
                    String previousDigest = existing.getContentDigest() == null
                            ? profileDigestCalculator.digest(existing)
                            : existing.getContentDigest();
                    if (replaceSkills) {
                        existing.setSkills(preferences.getSkills());
                    }
                    existing.setAspirations(preferences.getAspirations());
                    existing.setWorkPreferences(preferences.getWorkPreferences());
                    String updatedDigest = profileDigestCalculator.digest(existing);
                    if (!updatedDigest.equals(previousDigest)) {
                        existing.setRevision(existing.getRevision() + 1);
                        existing.setRevisionId(UUID.randomUUID().toString());
                    }
                    existing.setContentDigest(updatedDigest);
                    return userProfileRepository.save(existing);
                })
                .orElseGet(() -> {
                    if (expectedRevision != null && expectedRevision != 0) {
                        throw new ProfileRevisionConflictException();
                    }
                    preferences.setUserId(userId);
                    if (!replaceSkills) {
                        preferences.setSkills(new ArrayList<>());
                    }
                    preferences.setQualifications(new ArrayList<>());
                    preferences.setRoles(new ArrayList<>());
                    preferences.setRevision(1L);
                    preferences.setRevisionId(UUID.randomUUID().toString());
                    preferences.setContentDigest(profileDigestCalculator.digest(preferences));
                    return userProfileRepository.save(preferences);
                });
    }

    private UserProfile saveProfile(
            String userId,
            UserProfile userProfile,
            Long expectedRevision,
            long startedAt) {
        return userProfileRepository.findByUserId(userId)
                .map(existingProfile -> {
                    if (expectedRevision != null && expectedRevision != existingProfile.getRevision()) {
                        throw new ProfileRevisionConflictException();
                    }
                    String previousDigest = existingProfile.getContentDigest();
                    if (previousDigest == null) {
                        previousDigest = profileDigestCalculator.digest(existingProfile);
                    }
                    existingProfile.setSkills(userProfile.getSkills());
                    existingProfile.setAspirations(userProfile.getAspirations());
                    existingProfile.setWorkPreferences(userProfile.getWorkPreferences());
                    replaceQualifications(existingProfile, userProfile.getQualifications());
                    replaceRoles(existingProfile, userProfile.getRoles());
                    String updatedDigest = profileDigestCalculator.digest(existingProfile);
                    if (!updatedDigest.equals(previousDigest)) {
                        existingProfile.setRevision(existingProfile.getRevision() + 1);
                        existingProfile.setRevisionId(UUID.randomUUID().toString());
                    } else if (existingProfile.getRevisionId() == null) {
                        existingProfile.setRevisionId(UUID.randomUUID().toString());
                    }
                    existingProfile.setContentDigest(updatedDigest);
                    UserProfile saved = userProfileRepository.save(existingProfile);
                    log.info("User profile updated durationMs={}",
                            (System.nanoTime() - startedAt) / 1_000_000);
                    return saved;
                })
                .orElseGet(() -> {
                    if (expectedRevision != null && expectedRevision != 0) {
                        throw new ProfileRevisionConflictException();
                    }
                    userProfile.setId(null);
                    userProfile.setRevision(1L);
                    userProfile.setRevisionId(UUID.randomUUID().toString());
                    replaceQualifications(userProfile, userProfile.getQualifications());
                    replaceRoles(userProfile, userProfile.getRoles());
                    userProfile.setContentDigest(profileDigestCalculator.digest(userProfile));
                    UserProfile saved = userProfileRepository.save(userProfile);
                    log.info("User profile created durationMs={}",
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
