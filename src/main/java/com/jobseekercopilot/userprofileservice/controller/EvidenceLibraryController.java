package com.jobseekercopilot.userprofileservice.controller;

import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceEntry;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceSupersedeRequest;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceVisibility;
import com.jobseekercopilot.userprofileservice.model.evidence.EvidenceWriteRequest;
import com.jobseekercopilot.userprofileservice.service.EvidenceLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<EvidenceEntry> list(
            @AuthenticationPrincipal Jwt accessToken,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return evidenceLibraryService.listForOwner(accessToken.getSubject(), includeArchived);
    }

    @GetMapping("/{entryId}")
    @Operation(summary = "Get one evidence entry owned by the authenticated claimant")
    public ResponseEntity<EvidenceEntry> get(
            @AuthenticationPrincipal Jwt accessToken,
            @PathVariable String entryId) {
        return response(evidenceLibraryService.getForOwner(accessToken.getSubject(), entryId));
    }

    @PostMapping
    @Operation(summary = "Create a draft evidence entry")
    public ResponseEntity<EvidenceEntry> create(
            @AuthenticationPrincipal Jwt accessToken,
            @Valid @RequestBody EvidenceWriteRequest request) {
        EvidenceEntry entry = evidenceLibraryService.create(accessToken.getSubject(), request);
        return ResponseEntity.status(201)
                .eTag(Long.toString(entry.getVersion()))
                .body(entry);
    }

    @PutMapping("/{entryId}")
    @Operation(summary = "Create a new draft revision for an evidence entry")
    public ResponseEntity<EvidenceEntry> edit(
            @AuthenticationPrincipal Jwt accessToken,
            @PathVariable String entryId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody EvidenceWriteRequest request) {
        return response(evidenceLibraryService.edit(
                accessToken.getSubject(), entryId, parseVersion(ifMatch), request));
    }

    @PostMapping("/{entryId}/confirm")
    @Operation(summary = "Confirm the latest draft as a new immutable revision")
    public ResponseEntity<EvidenceEntry> confirm(
            @AuthenticationPrincipal Jwt accessToken,
            @PathVariable String entryId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(evidenceLibraryService.confirm(
                accessToken.getSubject(), entryId, parseVersion(ifMatch)));
    }

    @PostMapping("/{entryId}/hide")
    @Operation(summary = "Hide an evidence entry from routine selectors")
    public ResponseEntity<EvidenceEntry> hide(
            @AuthenticationPrincipal Jwt accessToken,
            @PathVariable String entryId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(evidenceLibraryService.setVisibility(
                accessToken.getSubject(), entryId, parseVersion(ifMatch), EvidenceVisibility.HIDDEN));
    }

    @PostMapping("/{entryId}/show")
    @Operation(summary = "Show an evidence entry in routine selectors")
    public ResponseEntity<EvidenceEntry> show(
            @AuthenticationPrincipal Jwt accessToken,
            @PathVariable String entryId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(evidenceLibraryService.setVisibility(
                accessToken.getSubject(), entryId, parseVersion(ifMatch), EvidenceVisibility.VISIBLE));
    }

    @PostMapping("/{entryId}/archive")
    @Operation(summary = "Archive an evidence entry while preserving historic references")
    public ResponseEntity<EvidenceEntry> archive(
            @AuthenticationPrincipal Jwt accessToken,
            @PathVariable String entryId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(evidenceLibraryService.archive(
                accessToken.getSubject(), entryId, parseVersion(ifMatch)));
    }

    @PostMapping("/{entryId}/restore")
    @Operation(summary = "Restore an archived evidence entry")
    public ResponseEntity<EvidenceEntry> restore(
            @AuthenticationPrincipal Jwt accessToken,
            @PathVariable String entryId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        return response(evidenceLibraryService.restore(
                accessToken.getSubject(), entryId, parseVersion(ifMatch)));
    }

    @PostMapping("/{entryId}/supersede")
    @Operation(summary = "Supersede an evidence entry with another owned active entry")
    public ResponseEntity<EvidenceEntry> supersede(
            @AuthenticationPrincipal Jwt accessToken,
            @PathVariable String entryId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody EvidenceSupersedeRequest request) {
        return response(evidenceLibraryService.supersede(
                accessToken.getSubject(),
                entryId,
                parseVersion(ifMatch),
                request.getReplacementEntryId()));
    }

    private ResponseEntity<EvidenceEntry> response(EvidenceEntry entry) {
        return ResponseEntity.ok()
                .eTag(Long.toString(entry.getVersion()))
                .body(entry);
    }

    private Long parseVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return null;
        }
        String candidate = ifMatch.strip();
        if (candidate.startsWith("W/")) {
            throw new IllegalArgumentException("Weak ETags are not supported");
        }
        if (candidate.length() >= 2 && candidate.startsWith("\"") && candidate.endsWith("\"")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        try {
            long version = Long.parseLong(candidate);
            if (version < 0) {
                throw new IllegalArgumentException("Evidence version cannot be negative");
            }
            return version;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("If-Match must contain an evidence version", exception);
        }
    }
}
