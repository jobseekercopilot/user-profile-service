package com.jobseekercopilot.userprofileservice.controller;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.service.UserProfileService;
import com.jobseekercopilot.userprofileservice.exception.ResourceNotFoundException;
import com.jobseekercopilot.userprofileservice.exception.ProfileWriteConflictException;
import com.jobseekercopilot.userprofileservice.observability.ProfileTelemetry;
import com.jobseekercopilot.userprofileservice.observability.ProfileTelemetry.OperationType;
import com.jobseekercopilot.userprofileservice.observability.ProfileTelemetry.Outcome;
import com.jobseekercopilot.userprofileservice.observability.ProfileTelemetry.StatusFamily;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "User Profiles", description = "Endpoints for managing user profiles")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final ProfileTelemetry telemetry;

    public UserProfileController(UserProfileService userProfileService, ProfileTelemetry telemetry) {
        this.userProfileService = userProfileService;
        this.telemetry = telemetry;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves the profile for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile found",
                    content = @Content(schema = @Schema(implementation = UserProfile.class))),
            @ApiResponse(responseCode = "404", description = "Profile not found for the given user ID")
    })
    public ResponseEntity<UserProfile> getMyProfile(
            @AuthenticationPrincipal Jwt accessToken) {
        String userId = accessToken.getSubject();
        long startedAt = System.nanoTime();
        try {
            var profile = userProfileService.getProfileByUserId(userId);
            if (profile.isPresent()) {
                telemetry.record(OperationType.READ, Outcome.SUCCESS, StatusFamily.SUCCESS, System.nanoTime() - startedAt);
                return new ResponseEntity<>(profile.get(), HttpStatus.OK);
            }
            telemetry.record(OperationType.READ, Outcome.NOT_FOUND, StatusFamily.CLIENT_ERROR, System.nanoTime() - startedAt);
            throw new ResourceNotFoundException("User profile not found");
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            telemetry.record(OperationType.READ, Outcome.INTERNAL_ERROR, StatusFamily.SERVER_ERROR, System.nanoTime() - startedAt);
            throw exception;
        }
    }

    @PutMapping("/me")
    @Operation(summary = "Create or update user profile", description = "Creates a new profile or updates an existing one for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile saved successfully",
                    content = @Content(schema = @Schema(implementation = UserProfile.class))),
            @ApiResponse(responseCode = "400", description = "Invalid profile data"),
            @ApiResponse(responseCode = "409", description = "Concurrent profile write conflict; retry is safe")
    })
    public ResponseEntity<UserProfile> createOrUpdateMyProfile(
            @AuthenticationPrincipal Jwt accessToken,
            @Valid @RequestBody UserProfile userProfile) {
        String userId = accessToken.getSubject();
        long startedAt = System.nanoTime();
        try {
            UserProfile savedProfile = userProfileService.createOrUpdateProfile(userId, userProfile);
            telemetry.record(OperationType.UPSERT, Outcome.SUCCESS, StatusFamily.SUCCESS, System.nanoTime() - startedAt);
            return new ResponseEntity<>(savedProfile, HttpStatus.OK);
        } catch (ProfileWriteConflictException exception) {
            telemetry.record(OperationType.UPSERT, Outcome.CONFLICT, StatusFamily.CLIENT_ERROR, System.nanoTime() - startedAt);
            throw exception;
        } catch (IllegalArgumentException exception) {
            telemetry.record(OperationType.UPSERT, Outcome.INVALID_REQUEST, StatusFamily.CLIENT_ERROR, System.nanoTime() - startedAt);
            throw exception;
        } catch (RuntimeException exception) {
            telemetry.record(OperationType.UPSERT, Outcome.INTERNAL_ERROR, StatusFamily.SERVER_ERROR, System.nanoTime() - startedAt);
            throw exception;
        }
    }
}
