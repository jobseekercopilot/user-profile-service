package com.jobseekercopilot.userprofileservice.controller;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private UserProfileController userProfileController;

    @Test
    void getMyProfile_ShouldReturnProfile() {
        String userId = "user-123";
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        when(userProfileService.getProfileByUserId(userId)).thenReturn(Optional.of(profile));

        ResponseEntity<UserProfile> response = userProfileController.getMyProfile(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userProfileService, times(1)).getProfileByUserId(userId);
    }

    @Test
    void getMyProfile_ShouldThrowNotFound_WhenMissing() {
        String userId = "unknown-user";
        when(userProfileService.getProfileByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(com.jobseekercopilot.userprofileservice.exception.ResourceNotFoundException.class,
                () -> userProfileController.getMyProfile(userId));
    }

    @Test
    void createOrUpdateMyProfile_ShouldReturnProfile() {
        String userId = "user-123";
        UserProfile inputProfile = new UserProfile();
        inputProfile.setUserId(userId);
        UserProfile savedProfile = new UserProfile();
        savedProfile.setUserId(userId);

        when(userProfileService.createOrUpdateProfile(userId, inputProfile)).thenReturn(savedProfile);

        ResponseEntity<UserProfile> response = userProfileController.createOrUpdateMyProfile(userId, inputProfile);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userProfileService, times(1)).createOrUpdateProfile(userId, inputProfile);
    }
}
