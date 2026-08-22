package com.jobseekercopilot.userprofileservice.controller;

import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshot;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSnapshotRequest;
import com.jobseekercopilot.userprofileservice.service.EvidenceSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evidence/snapshots")
@Tag(name = "Evidence snapshots", description = "Purpose-bound immutable evidence selections")
@SecurityRequirement(name = "bearerAuth")
public class EvidenceSnapshotController {

    private final EvidenceSnapshotService snapshotService;

    public EvidenceSnapshotController(EvidenceSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @PostMapping
    @Operation(
            operationId = "createEvidenceSnapshot",
            summary = "Create an immutable snapshot from explicitly selected eligible evidence")
    public ResponseEntity<EvidenceSnapshot> create(
            @AuthenticationPrincipal Jwt accessToken,
            @Valid @RequestBody EvidenceSnapshotRequest request) {
        return ResponseEntity.status(201)
                .body(snapshotService.create(accessToken.getSubject(), request));
    }

    @GetMapping("/{snapshotId}")
    @Operation(
            operationId = "getEvidenceSnapshot",
            summary = "Get an immutable evidence snapshot owned by the authenticated claimant")
    public EvidenceSnapshot get(
            @AuthenticationPrincipal Jwt accessToken,
            @PathVariable String snapshotId) {
        return snapshotService.getForOwner(accessToken.getSubject(), snapshotId);
    }
}
