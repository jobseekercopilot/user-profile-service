package com.jobseekercopilot.userprofileservice.controller;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.service.UserProfileService;
import com.jobseekercopilot.userprofileservice.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    private static final String USER_ID_HEADER = "X-User-Id";

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getMyProfile(@RequestHeader(USER_ID_HEADER) String userId) {
        return userProfileService.getProfileByUserId(userId)
                .map(userProfile -> new ResponseEntity<>(userProfile, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for userId: " + userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfile> createOrUpdateMyProfile(@RequestHeader(USER_ID_HEADER) String userId, @RequestBody UserProfile userProfile) {
        UserProfile savedProfile = userProfileService.createOrUpdateProfile(userId, userProfile);
        return new ResponseEntity<>(savedProfile, HttpStatus.OK);
    }
}