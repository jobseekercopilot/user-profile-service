package com.jobseekercopilot.userprofileservice.controller;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.service.UserProfileService;
import com.jobseekercopilot.userprofileservice.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "User Profiles", description = "Endpoints for managing user profiles")
@Validated
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    private static final String USER_ID_HEADER = "X-User-Id";

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves the profile for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile found",
                    content = @Content(schema = @Schema(implementation = UserProfile.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found for the given user ID")
    })
    public ResponseEntity<UserProfile> getMyProfile(
            @Parameter(description = "User ID from authentication header")
            @RequestHeader(USER_ID_HEADER) @NotBlank @Size(max = 128) String userId) {
        return userProfileService.getProfileByUserId(userId.strip())
                .map(userProfile -> new ResponseEntity<>(userProfile, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
    }

    @PutMapping("/me")
    @Operation(summary = "Create or update user profile", description = "Creates a new profile or updates an existing one for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile saved successfully",
                    content = @Content(schema = @Schema(implementation = UserProfile.class))),
            @ApiResponse(responseCode = "400", description = "Invalid profile data")
    })
    public ResponseEntity<UserProfile> createOrUpdateMyProfile(
            @Parameter(description = "User ID from authentication header")
            @RequestHeader(USER_ID_HEADER) @NotBlank @Size(max = 128) String userId,
            @Valid @RequestBody UserProfile userProfile) {
        UserProfile savedProfile = userProfileService.createOrUpdateProfile(userId.strip(), userProfile);
        return new ResponseEntity<>(savedProfile, HttpStatus.OK);
    }
}
