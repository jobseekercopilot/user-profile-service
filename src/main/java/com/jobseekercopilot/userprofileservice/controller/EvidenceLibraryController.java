package com.jobseekercopilot.userprofileservice.controller;

import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.service.EvidenceLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evidence")
@Tag(name = "Evidence Library", description = "Owner-scoped reusable claimant evidence")
@SecurityRequirement(name = "bearerAuth")
public class EvidenceLibraryController {

    private final EvidenceLibraryService evidenceLibraryService;

    public EvidenceLibraryController(EvidenceLibraryService evidenceLibraryService) {
        this.evidenceLibraryService = evidenceLibraryService;
    }

    @GetMapping
    @Operation(summary = "List the authenticated claimant's evidence entries")
    public List<EvidenceEntry> list(@AuthenticationPrincipal Jwt accessToken) {
        return evidenceLibraryService.listForOwner(accessToken.getSubject());
    }

    @GetMapping("/{entryId}")
    @Operation(summary = "Get one evidence entry owned by the authenticated claimant")
    public EvidenceEntry get(
            @AuthenticationPrincipal Jwt accessToken,
            @PathVariable String entryId) {
        return evidenceLibraryService.getForOwner(accessToken.getSubject(), entryId);
    }
}
