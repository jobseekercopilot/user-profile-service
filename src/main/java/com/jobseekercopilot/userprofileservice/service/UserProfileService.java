package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public Optional<UserProfile> getProfileByUserId(String userId) {
        return userProfileRepository.findByUserId(userId);
    }

    public UserProfile createOrUpdateProfile(String userId, UserProfile userProfile) {
        if (userProfile == null) {
            throw new IllegalArgumentException("User profile cannot be null");
        }
        userProfile.setUserId(userId);
        return userProfileRepository.findByUserId(userId)
                .map(existingProfile -> {
                    existingProfile.setSkills(userProfile.getSkills());
                    existingProfile.setExperience(userProfile.getExperience());
                    existingProfile.setAspirations(userProfile.getAspirations());
                    existingProfile.setWorkPrefs(userProfile.getWorkPrefs());
                    return userProfileRepository.save(existingProfile);
                })
                .orElseGet(() -> userProfileRepository.save(userProfile));
    }
}