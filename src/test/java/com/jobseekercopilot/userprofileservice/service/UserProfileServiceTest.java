package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.Aspirations;
import com.jobseekercopilot.userprofileservice.model.WorkPreferences;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import com.jobseekercopilot.userprofileservice.validation.QualificationValidator;
import com.jobseekercopilot.userprofileservice.validation.RoleValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private QualificationValidator qualificationValidator;

    @Mock
    private RoleValidator roleValidator;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void getProfileByUserId_ShouldReturnProfile() {
        String userId = "user-123";
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setUserId(userId);
        profile.setSkills(List.of("Java"));
        Aspirations aspirations = new Aspirations();
        aspirations.setTargetRoles(List.of("Lead"));
        profile.setAspirations(aspirations);
        WorkPreferences workPreferences = new WorkPreferences();
        profile.setWorkPreferences(workPreferences);
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Optional<UserProfile> result = userProfileService.getProfileByUserId(userId);

        assertTrue(result.isPresent());
        assertEquals(List.of("Java"), result.get().getSkills());
        verify(userProfileRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getProfileByUserId_ShouldReturnEmpty_WhenNotFound() {
        when(userProfileRepository.findByUserId("unknown")).thenReturn(Optional.empty());

        Optional<UserProfile> result = userProfileService.getProfileByUserId("unknown");

        assertFalse(result.isPresent());
    }

    @Test
    void createOrUpdateProfile_ShouldCreate_WhenNotExists() {
        String userId = "user-123";
        UserProfile newProfile = new UserProfile();
        newProfile.setUserId(userId);
        newProfile.setSkills(List.of("Java"));
        Aspirations aspirations = new Aspirations();
        aspirations.setTargetRoles(List.of("Developer"));
        newProfile.setAspirations(aspirations);
        WorkPreferences workPreferences = new WorkPreferences();
        newProfile.setWorkPreferences(workPreferences);

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile result = userProfileService.createOrUpdateProfile(userId, newProfile);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(List.of("Java"), result.getSkills());
        verify(userProfileRepository, times(1)).findByUserId(userId);
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void createOrUpdateProfile_ShouldUpdate_WhenExists() {
        String userId = "user-123";
        UserProfile existingProfile = new UserProfile();
        existingProfile.setId(1L);
        existingProfile.setUserId(userId);
        existingProfile.setSkills(List.of("Java"));
        Aspirations aspirations1 = new Aspirations();
        aspirations1.setTargetRoles(List.of("Junior"));
        existingProfile.setAspirations(aspirations1);
        WorkPreferences workPreferences1 = new WorkPreferences();
        existingProfile.setWorkPreferences(workPreferences1);

        UserProfile updateProfile = new UserProfile();
        updateProfile.setUserId(userId);
        updateProfile.setSkills(List.of("Kotlin"));
        Aspirations aspirations2 = new Aspirations();
        aspirations2.setTargetRoles(List.of("Senior"));
        updateProfile.setAspirations(aspirations2);
        WorkPreferences workPreferences2 = new WorkPreferences();
        updateProfile.setWorkPreferences(workPreferences2);

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile result = userProfileService.createOrUpdateProfile(userId, updateProfile);

        assertNotNull(result);
        assertEquals(List.of("Kotlin"), result.getSkills());
        verify(userProfileRepository, times(1)).save(existingProfile);
    }

    @Test
    void createOrUpdateProfile_ShouldThrow_WhenProfileNull() {
        assertThrows(IllegalArgumentException.class,
                () -> userProfileService.createOrUpdateProfile("user-123", null));
        verify(userProfileRepository, never()).save(any());
    }
}