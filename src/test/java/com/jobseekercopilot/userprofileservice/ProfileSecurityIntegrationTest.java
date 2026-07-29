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

    private ResponseEntity<Map> put(HttpHeaders headers, String body) {
        return restTemplate.exchange("/api/profiles/me", HttpMethod.PUT,
                new HttpEntity<>(body, headers), Map.class);
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
