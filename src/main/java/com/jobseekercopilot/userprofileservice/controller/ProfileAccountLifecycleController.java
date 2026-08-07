package com.jobseekercopilot.userprofileservice.controller;

import com.jobseekercopilot.userprofileservice.dto.ProfilePersonalDataExport;
import com.jobseekercopilot.userprofileservice.service.ProfileAccountLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfileAccountLifecycleController {

    private final ProfileAccountLifecycleService lifecycleService;

    @GetMapping("/api/profiles/me/export")
    @Operation(
            summary = "Export current owner's profile data",
            description = "Returns a synchronous machine-readable export and stores no artifact.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProfilePersonalDataExport> export(
            @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(lifecycleService.export(token.getSubject()));
    }

    @DeleteMapping("/internal/account-lifecycle/personal-data")
    @Operation(
            summary = "Erase profile-owned personal data",
            description = "Idempotent coordinator-only deletion using a signed account-lifecycle token.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> erase(@AuthenticationPrincipal Jwt token) {
        lifecycleService.erase(token.getSubject());
        return ResponseEntity.noContent().build();
    }
}
