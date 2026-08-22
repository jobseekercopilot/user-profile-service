package com.jobseekercopilot.userprofileservice;

import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ProfileSecurityIntegrationTest {

    private static final TestJwksServer JWKS = new TestJwksServer();

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("profile.security.jwk-set-uri", JWKS::jwkSetUri);
        registry.add("profile.security.issuer", () -> TestJwksServer.ISSUER);
        registry.add("profile.security.audience", () -> TestJwksServer.AUDIENCE);
    }

    @AfterAll
    static void stopJwks() {
        JWKS.close();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserProfileRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void validTokenDerivesOwnerAndIgnoresForgedHeaderAndBodyIdentity() {
        HttpHeaders headers = authenticated(JWKS.validToken("alice"));
        headers.set("X-User-Id", "victim");
        ResponseEntity<Map> response = put(headers, "{\"userId\":\"victim\",\"skills\":[\"Java\"]}");

        assertEquals(HttpStatus.OK, response.getStatusCode(), String.valueOf(response.getBody()));
        assertEquals("alice", response.getBody().get("userId"));
        assertTrue(repository.findByUserId("alice").isPresent());
        assertTrue(repository.findByUserId("victim").isEmpty());
    }

    @Test
    void oneUsersTokenCannotReadOrOverwriteAnotherUsersProfile() {
        ResponseEntity<Map> aliceWrite = put(authenticated(JWKS.validToken("alice")),
                "{\"skills\":[\"Alice skill\"]}");
        assertEquals(HttpStatus.OK, aliceWrite.getStatusCode(), String.valueOf(aliceWrite.getBody()));

        HttpHeaders bob = authenticated(JWKS.validToken("bob"));
        bob.set("X-User-Id", "alice");
        ResponseEntity<Map> read = restTemplate.exchange(
                "/api/profiles/me", HttpMethod.GET, new HttpEntity<>(bob), Map.class);
        ResponseEntity<Map> write = put(bob, "{\"userId\":\"alice\",\"skills\":[\"Bob skill\"]}");

        assertEquals(HttpStatus.NOT_FOUND, read.getStatusCode());
        assertEquals(HttpStatus.OK, write.getStatusCode());
        assertEquals("bob", write.getBody().get("userId"));
        ResponseEntity<Map> aliceProfile = restTemplate.exchange(
                "/api/profiles/me", HttpMethod.GET,
                new HttpEntity<>(authenticated(JWKS.validToken("alice"))), Map.class);
        ResponseEntity<Map> bobProfile = restTemplate.exchange(
                "/api/profiles/me", HttpMethod.GET,
                new HttpEntity<>(authenticated(JWKS.validToken("bob"))), Map.class);
        assertEquals(List.of("Alice skill"), aliceProfile.getBody().get("skills"));
        assertEquals(List.of("Bob skill"), bobProfile.getBody().get("skills"));
    }

    @Test
    void missingMalformedExpiredForgedAndConstrainedTokensFailUniformly() {
        List<String> invalidTokens = List.of(
                "not-a-jwt",
                JWKS.expiredToken("expired-subject"),
                JWKS.forgedKnownKeyToken("forged-subject"),
                JWKS.unknownKeyToken("unknown-key-subject"),
                JWKS.wrongAlgorithmToken("wrong-algorithm-subject"),
                JWKS.wrongIssuerToken("wrong-issuer-subject"),
                JWKS.wrongAudienceToken("wrong-audience-subject"),
                JWKS.refreshTokenType("wrong-type-subject"));

        assertAuthenticationFailure(new HttpHeaders(), null);
        for (String token : invalidTokens) {
            assertAuthenticationFailure(authenticated(token), token);
        }
    }

    @Test
    void healthRemainsPublicAndUnrelatedRoutesAreDenied() {
        assertEquals(HttpStatus.OK, restTemplate.getForEntity("/actuator/health", Map.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                restTemplate.getForEntity("/not-an-application-route", Map.class).getStatusCode());
    }

    @Test
    void accountLifecycleTokensAreConfinedToDeletionAndExportsAreNoStore() {
        HttpHeaders access = authenticated(JWKS.validToken("lifecycle-owner"));
        assertEquals(HttpStatus.OK, put(access, "{\"skills\":[\"Java\"]}").getStatusCode());

        ResponseEntity<Map> export = restTemplate.exchange(
                "/api/profiles/me/export",
                HttpMethod.GET,
                new HttpEntity<>(access),
                Map.class);
        assertEquals(HttpStatus.OK, export.getStatusCode(), String.valueOf(export.getBody()));
        assertTrue(export.getHeaders().getCacheControl().contains("no-store"));
        assertEquals("profile-personal-data.v1", export.getBody().get("schemaVersion"));

        HttpHeaders lifecycle = authenticated(
                JWKS.accountLifecycleToken("lifecycle-owner", "operation-123"));
        assertEquals(HttpStatus.FORBIDDEN, restTemplate.exchange(
                "/api/profiles/me/export",
                HttpMethod.GET,
                new HttpEntity<>(lifecycle),
                Map.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, restTemplate.exchange(
                "/internal/account-lifecycle/personal-data",
                HttpMethod.DELETE,
                new HttpEntity<>(access),
                Void.class).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, restTemplate.exchange(
                "/internal/account-lifecycle/personal-data",
                HttpMethod.DELETE,
                new HttpEntity<>(lifecycle),
                Void.class).getStatusCode());
        assertTrue(repository.findByUserId("lifecycle-owner").isEmpty());
    }

    @Test
    void profileRevisionRejectsStaleWritesAndKeepsMissingPreferencesUnset() {
        HttpHeaders headers = authenticated(JWKS.validToken("revision-owner"));
        ResponseEntity<Map> created = put(headers, "{\"skills\":[\"Java\"]}");

        assertEquals(HttpStatus.OK, created.getStatusCode(), String.valueOf(created.getBody()));
        assertEquals(1, created.getBody().get("revision"));
        assertEquals("\"1\"", created.getHeaders().getETag());
        assertNull(created.getBody().get("aspirations"));
        assertNull(created.getBody().get("workPreferences"));

        HttpHeaders current = authenticated(JWKS.validToken("revision-owner"));
        current.setIfMatch("\"1\"");
        ResponseEntity<Map> updated = put(current, "{\"skills\":[\"Java\",\"SQL\"]}");
        assertEquals(HttpStatus.OK, updated.getStatusCode(), String.valueOf(updated.getBody()));
        assertEquals(2, updated.getBody().get("revision"));
        assertEquals("\"2\"", updated.getHeaders().getETag());

        ResponseEntity<Map> stale = put(current, "{\"skills\":[\"Invented stale value\"]}");
        assertEquals(HttpStatus.CONFLICT, stale.getStatusCode());
        assertEquals("PROFILE_REVISION_CONFLICT", stale.getBody().get("code"));

        ResponseEntity<Map> retained = restTemplate.exchange(
                "/api/profiles/me",
                HttpMethod.GET,
                new HttpEntity<>(authenticated(JWKS.validToken("revision-owner"))),
                Map.class);
        assertEquals(List.of("Java", "SQL"), retained.getBody().get("skills"));
        assertEquals(2, retained.getBody().get("revision"));
    }

    @Test
    void legacyEvidenceMigrationIsIdempotentDraftAndOwnerScoped() {
        HttpHeaders owner = authenticated(JWKS.validToken("evidence-owner"));
        String body = """
                {
                  "roles": [{
                    "jobTitle": "Engineer",
                    "employer": "Employer",
                    "status": "CURRENT",
                    "startDate": "2025-01",
                    "keyResponsibilities": "Built services"
                  }],
                  "qualifications": [{
                    "qualificationName": "Cloud certificate",
                    "issuingBody": "Training provider",
                    "status": "COMPLETED",
                    "grade": "Pass",
                    "dateAchieved": "2025-06"
                  }]
                }
                """;
        assertEquals(HttpStatus.OK, put(owner, body).getStatusCode());

        ResponseEntity<List> first = restTemplate.exchange(
                "/api/evidence",
                HttpMethod.GET,
                new HttpEntity<>(owner),
                List.class);
        ResponseEntity<List> rerun = restTemplate.exchange(
                "/api/evidence",
                HttpMethod.GET,
                new HttpEntity<>(owner),
                List.class);

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals(2, first.getBody().size());
        assertEquals(2, rerun.getBody().size());
        Map firstEntry = (Map) first.getBody().get(0);
        assertEquals(true, firstEntry.get("reviewRequired"));
        assertEquals("ACTIVE", firstEntry.get("lifecycle"));
        Map firstRevision = (Map) ((List) firstEntry.get("revisions")).get(0);
        assertEquals("DRAFT", firstRevision.get("confirmationState"));
        assertEquals("LEGACY_MIGRATION", firstRevision.get("createdBy"));
        assertNotNull(firstRevision.get("contentDigest"));
        assertFalse(((List) firstRevision.get("facts")).isEmpty());

        ResponseEntity<Map> crossOwner = restTemplate.exchange(
                "/api/evidence/" + firstEntry.get("entryId"),
                HttpMethod.GET,
                new HttpEntity<>(authenticated(JWKS.validToken("different-owner"))),
                Map.class);
        assertEquals(HttpStatus.NOT_FOUND, crossOwner.getStatusCode());
    }

    @Test
    void progressivePreferenceUpdatePreservesHistoryAndDoesNotCreateDefaults() {
        HttpHeaders owner = authenticated(JWKS.validToken("progressive-owner"));
        String legacy = """
                {
                  "skills": ["Java"],
                  "roles": [{
                    "jobTitle": "Analyst",
                    "employer": "Employer",
                    "status": "CURRENT",
                    "startDate": "2024-01"
                  }],
                  "qualifications": [{
                    "qualificationName": "Certificate",
                    "issuingBody": "Provider",
                    "status": "COMPLETED",
                    "grade": "Pass",
                    "dateAchieved": "2024-02"
                  }]
                }
                """;
        ResponseEntity<Map> created = put(owner, legacy);
        assertEquals(HttpStatus.OK, created.getStatusCode(), String.valueOf(created.getBody()));

        HttpHeaders updateHeaders = authenticated(JWKS.validToken("progressive-owner"));
        updateHeaders.setIfMatch("\"1\"");
        ResponseEntity<Map> updated = restTemplate.exchange(
                "/api/profiles/me",
                HttpMethod.PATCH,
                new HttpEntity<>("""
                        {
                          "aspirations": {"targetRoles": ["Platform Engineer"]},
                          "workPreferences": {
                            "employmentTypes": ["PERMANENT"],
                            "workingPatterns": ["FLEXIBLE"],
                            "workplaceArrangements": ["HYBRID", "REMOTE"],
                            "noticePeriodDays": 30
                          }
                        }
                        """, updateHeaders),
                Map.class);

        assertEquals(HttpStatus.OK, updated.getStatusCode(), String.valueOf(updated.getBody()));
        assertEquals(2, updated.getBody().get("revision"));
        assertEquals(List.of("Java"), updated.getBody().get("skills"));
        assertEquals(1, ((List) updated.getBody().get("roles")).size());
        assertEquals(1, ((List) updated.getBody().get("qualifications")).size());
        Map preferences = (Map) updated.getBody().get("workPreferences");
        assertEquals(List.of("PERMANENT"), preferences.get("employmentTypes"));
        assertEquals(List.of("FLEXIBLE"), preferences.get("workingPatterns"));
        assertEquals(30, preferences.get("noticePeriodDays"));
        assertNull(preferences.get("commuteRange"));
        assertNull(((Map) updated.getBody().get("aspirations")).get("targetWeeklyHours"));
    }

    @Test
    void evidenceLifecycleCreatesImmutableDraftsAndRejectsStaleOrCrossOwnerActions() {
        HttpHeaders owner = authenticated(JWKS.validToken("library-owner"));
        assertEquals(HttpStatus.OK, put(owner, "{}").getStatusCode());

        ResponseEntity<Map> created = evidence(
                "/api/evidence",
                HttpMethod.POST,
                owner,
                """
                        {
                          "category": "PROJECT",
                          "heading": "Portfolio project",
                          "description": "Built an accessible service",
                          "demonstratedSkills": ["Java"],
                          "supportingLinks": ["https://example.org/project"]
                        }
                        """);
        assertEquals(HttpStatus.CREATED, created.getStatusCode(), String.valueOf(created.getBody()));
        assertEquals("\"0\"", created.getHeaders().getETag());
        String entryId = (String) created.getBody().get("entryId");
        assertEquals("DRAFT", latestRevision(created).get("confirmationState"));

        HttpHeaders editHeaders = authenticated(JWKS.validToken("library-owner"));
        editHeaders.setIfMatch("\"0\"");
        ResponseEntity<Map> edited = evidence(
                "/api/evidence/" + entryId,
                HttpMethod.PUT,
                editHeaders,
                """
                        {
                          "category": "PROJECT",
                          "heading": "Portfolio project",
                          "description": "Built an accessible service and tests",
                          "demonstratedSkills": ["Java", "Testing"]
                        }
                        """);
        assertEquals(HttpStatus.OK, edited.getStatusCode(), String.valueOf(edited.getBody()));
        assertEquals(2, ((List) edited.getBody().get("revisions")).size());
        assertEquals("DRAFT", latestRevision(edited).get("confirmationState"));

        HttpHeaders confirmHeaders = authenticated(JWKS.validToken("library-owner"));
        confirmHeaders.setIfMatch(edited.getHeaders().getETag());
        ResponseEntity<Map> confirmed = evidence(
                "/api/evidence/" + entryId + "/confirm",
                HttpMethod.POST,
                confirmHeaders,
                null);
        assertEquals(HttpStatus.OK, confirmed.getStatusCode(), String.valueOf(confirmed.getBody()));
        assertEquals("USER_CONFIRMED", latestRevision(confirmed).get("confirmationState"));
        assertEquals(false, confirmed.getBody().get("reviewRequired"));

        ResponseEntity<Map> stale = evidence(
                "/api/evidence/" + entryId + "/archive",
                HttpMethod.POST,
                confirmHeaders,
                null);
        assertEquals(HttpStatus.CONFLICT, stale.getStatusCode(), String.valueOf(stale.getBody()));
        assertEquals("EVIDENCE_CONFLICT", stale.getBody().get("code"));

        HttpHeaders archiveHeaders = authenticated(JWKS.validToken("library-owner"));
        archiveHeaders.setIfMatch(confirmed.getHeaders().getETag());
        ResponseEntity<Map> archived = evidence(
                "/api/evidence/" + entryId + "/archive",
                HttpMethod.POST,
                archiveHeaders,
                null);
        assertEquals("ARCHIVED", archived.getBody().get("lifecycle"));

        ResponseEntity<List> normalList = restTemplate.exchange(
                "/api/evidence", HttpMethod.GET, new HttpEntity<>(owner), List.class);
        ResponseEntity<List> archivedList = restTemplate.exchange(
                "/api/evidence?includeArchived=true",
                HttpMethod.GET,
                new HttpEntity<>(owner),
                List.class);
        assertTrue(normalList.getBody().isEmpty());
        assertEquals(1, archivedList.getBody().size());

        ResponseEntity<Map> crossOwner = evidence(
                "/api/evidence/" + entryId + "/restore",
                HttpMethod.POST,
                authenticated(JWKS.validToken("different-owner")),
                null);
        assertEquals(HttpStatus.NOT_FOUND, crossOwner.getStatusCode());
        assertEquals("EVIDENCE_NOT_FOUND", crossOwner.getBody().get("code"));
    }

    @Test
    void evidenceSnapshotsArePurposeBoundImmutableAndOwnerScoped() {
        HttpHeaders owner = authenticated(JWKS.validToken("snapshot-owner"));
        assertEquals(HttpStatus.OK, put(owner, "{}").getStatusCode());

        ResponseEntity<Map> created = evidence(
                "/api/evidence",
                HttpMethod.POST,
                owner,
                """
                        {
                          "category": "PROJECT",
                          "heading": "Community accessibility project",
                          "description": "Built and tested an accessible search prototype",
                          "achievements": "Delivered a working prototype",
                          "demonstratedSkills": ["Accessibility", "Testing"]
                        }
                        """);
        HttpHeaders confirmHeaders = authenticated(JWKS.validToken("snapshot-owner"));
        confirmHeaders.setIfMatch(created.getHeaders().getETag());
        ResponseEntity<Map> confirmed = evidence(
                "/api/evidence/" + created.getBody().get("entryId") + "/confirm",
                HttpMethod.POST,
                confirmHeaders,
                null);
        assertEquals(HttpStatus.OK, confirmed.getStatusCode(), String.valueOf(confirmed.getBody()));

        String entryId = (String) confirmed.getBody().get("entryId");
        ResponseEntity<Map> snapshot = evidence(
                "/api/evidence/snapshots",
                HttpMethod.POST,
                owner,
                """
                        {
                          "purpose": "CV",
                          "entryIds": ["%s"],
                          "sectionOrder": ["PROJECT"]
                        }
                        """.formatted(entryId));

        assertEquals(HttpStatus.CREATED, snapshot.getStatusCode(), String.valueOf(snapshot.getBody()));
        assertEquals("CV", snapshot.getBody().get("purpose"));
        assertEquals(64, ((String) snapshot.getBody().get("snapshotDigest")).length());
        Map selection = (Map) ((List) snapshot.getBody().get("selections")).get(0);
        Map confirmedRevision = latestRevision(confirmed);
        assertEquals(confirmedRevision.get("revisionId"), selection.get("revisionId"));
        assertEquals(confirmedRevision.get("contentDigest"), selection.get("contentDigest"));
        assertFalse(((List) selection.get("facts")).isEmpty());
        assertEquals(
                ((Map) ((List) confirmedRevision.get("facts")).get(0)).get("factId"),
                ((Map) ((List) selection.get("facts")).get(0)).get("factId"));

        String snapshotId = (String) snapshot.getBody().get("snapshotId");
        ResponseEntity<Map> read = evidence(
                "/api/evidence/snapshots/" + snapshotId,
                HttpMethod.GET,
                owner,
                null);
        ResponseEntity<Map> crossOwner = evidence(
                "/api/evidence/snapshots/" + snapshotId,
                HttpMethod.GET,
                authenticated(JWKS.validToken("other-owner")),
                null);
        assertEquals(snapshot.getBody().get("snapshotDigest"), read.getBody().get("snapshotDigest"));
        assertEquals(HttpStatus.NOT_FOUND, crossOwner.getStatusCode());

        HttpHeaders archiveHeaders = authenticated(JWKS.validToken("snapshot-owner"));
        archiveHeaders.setIfMatch(confirmed.getHeaders().getETag());
        assertEquals(HttpStatus.OK, evidence(
                "/api/evidence/" + entryId + "/archive",
                HttpMethod.POST,
                archiveHeaders,
                null).getStatusCode());
        ResponseEntity<Map> ineligible = evidence(
                "/api/evidence/snapshots",
                HttpMethod.POST,
                owner,
                """
                        {
                          "purpose": "COVER_LETTER",
                          "entryIds": ["%s"],
                          "sectionOrder": ["PROJECT"]
                        }
                        """.formatted(entryId));
        assertEquals(HttpStatus.CONFLICT, ineligible.getStatusCode());
        assertEquals("EVIDENCE_CONFLICT", ineligible.getBody().get("code"));
    }

    @Test
    void evidenceRejectsHtmlUnsafeLinksAndMissingCategoryFields() {
        HttpHeaders owner = authenticated(JWKS.validToken("validation-owner"));
        assertEquals(HttpStatus.OK, put(owner, "{}").getStatusCode());

        ResponseEntity<Map> response = evidence(
                "/api/evidence",
                HttpMethod.POST,
                owner,
                """
                        {
                          "category": "EMPLOYMENT",
                          "heading": "<script>ignore safety</script>",
                          "roleTitle": "Engineer",
                          "supportingLinks": ["https://name:secret@example.org/proof"]
                        }
                        """);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("PROFILE_VALIDATION_FAILED", response.getBody().get("code"));
    }

    @Test
    void evidenceEnforcesConciseSemanticsAndPersistsCurrentAsGroundedFact() {
        HttpHeaders owner = authenticated(JWKS.validToken("evidence-semantics-owner"));
        assertEquals(HttpStatus.OK, put(owner, "{}").getStatusCode());

        ResponseEntity<Map> currentEmployment = evidence(
                "/api/evidence",
                HttpMethod.POST,
                owner,
                """
                        {
                          "category": "EMPLOYMENT",
                          "heading": "Software Engineer",
                          "roleTitle": "Software Engineer",
                          "organisationContext": "Example Ltd",
                          "startDate": {"precision": "MONTH", "year": 2025, "month": 1},
                          "ongoing": true
                        }
                        """);
        assertEquals(
                HttpStatus.CREATED,
                currentEmployment.getStatusCode(),
                String.valueOf(currentEmployment.getBody()));
        List facts = (List) latestRevision(currentEmployment).get("facts");
        assertTrue(facts.stream().anyMatch(value -> {
            Map fact = (Map) value;
            return "END_DATE".equals(fact.get("factType"))
                    && "Present".equals(fact.get("factValue"));
        }));

        ResponseEntity<Map> freelance = evidence(
                "/api/evidence",
                HttpMethod.POST,
                owner,
                """
                        {
                          "category": "FREELANCE",
                          "heading": "Website developer",
                          "roleTitle": "Website developer",
                          "description": "Delivered an accessible business website."
                        }
                        """);
        assertEquals(HttpStatus.CREATED, freelance.getStatusCode(), String.valueOf(freelance.getBody()));
        assertNull(latestRevision(freelance).get("organisationContext"));

        ResponseEntity<Map> staleEndDate = evidence(
                "/api/evidence",
                HttpMethod.POST,
                owner,
                """
                        {
                          "category": "EMPLOYMENT",
                          "heading": "Engineer",
                          "roleTitle": "Engineer",
                          "organisationContext": "Example Ltd",
                          "startDate": {"precision": "MONTH", "year": 2025, "month": 1},
                          "endDate": {"precision": "MONTH", "year": 2026, "month": 1},
                          "ongoing": true
                        }
                        """);
        assertEquals(HttpStatus.BAD_REQUEST, staleEndDate.getStatusCode());
        assertEquals("PROFILE_VALIDATION_FAILED", staleEndDate.getBody().get("code"));
        assertFalse(staleEndDate.getBody().toString().contains("Example Ltd"));
    }

    private ResponseEntity<Map> put(HttpHeaders headers, String body) {
        return restTemplate.exchange("/api/profiles/me", HttpMethod.PUT,
                new HttpEntity<>(body, headers), Map.class);
    }

    private ResponseEntity<Map> evidence(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body) {
        return restTemplate.exchange(
                path,
                method,
                body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers),
                Map.class);
    }

    private Map latestRevision(ResponseEntity<Map> response) {
        List revisions = (List) response.getBody().get("revisions");
        return (Map) revisions.get(revisions.size() - 1);
    }

    private void assertAuthenticationFailure(HttpHeaders headers, String token) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", "profile-security-test");
        ResponseEntity<Map> response = put(headers, "{}");
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("1", response.getBody().get("schemaVersion"));
        assertEquals("PROFILE_AUTHENTICATION_REQUIRED", response.getBody().get("code"));
        assertEquals("A valid Bearer access token is required.", response.getBody().get("message"));
        assertEquals("profile-security-test", response.getBody().get("correlationId"));
        if (token != null) {
            assertFalse(response.getBody().toString().contains(token));
        }
        assertFalse(response.getBody().toString().contains("subject"));
        assertFalse(response.getBody().toString().contains("Jwt"));
    }

    private static HttpHeaders authenticated(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
