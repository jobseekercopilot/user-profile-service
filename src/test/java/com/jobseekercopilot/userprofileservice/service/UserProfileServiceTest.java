package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void getProfileByUserId_ShouldReturnProfile() {
        String userId = "user-123";
        UserProfile profile = new UserProfile(1L, userId, "Java", "5 years", "Lead", "Remote");
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        Optional<UserProfile> result = userProfileService.getProfileByUserId(userId);

        assertTrue(result.isPresent());
        assertEquals("Java", result.get().getSkills());
        assertEquals("5 years", result.get().getExperience());
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
        UserProfile newProfile = new UserProfile(null, userId, "Java", "3 years", "Developer", "Hybrid");

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile result = userProfileService.createOrUpdateProfile(userId, newProfile);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("Java", result.getSkills());
        verify(userProfileRepository, times(1)).findByUserId(userId);
        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    @Test
    void createOrUpdateProfile_ShouldUpdate_WhenExists() {
        String userId = "user-123";
        UserProfile existingProfile = new UserProfile(1L, userId, "Java", "2 years", "Junior", "Office");
        UserProfile updateProfile = new UserProfile(null, userId, "Kotlin", "3 years", "Senior", "Remote");

        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile result = userProfileService.createOrUpdateProfile(userId, updateProfile);

        assertNotNull(result);
        assertEquals("Kotlin", result.getSkills());
        assertEquals("3 years", result.getExperience());
        assertEquals("Senior", result.getAspirations());
        assertEquals("Remote", result.getWorkPrefs());
        verify(userProfileRepository, times(1)).save(existingProfile);
    }

    @Test
    void createOrUpdateProfile_ShouldThrow_WhenProfileNull() {
        assertThrows(IllegalArgumentException.class,
                () -> userProfileService.createOrUpdateProfile("user-123", null));
        verify(userProfileRepository, never()).save(any());
    }
}