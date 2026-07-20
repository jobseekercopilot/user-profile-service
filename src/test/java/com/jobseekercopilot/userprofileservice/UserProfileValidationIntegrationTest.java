package com.jobseekercopilot.userprofileservice;

import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
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

import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class UserProfileValidationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserProfileRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void acceptsAndNormalizesValidUnicodeProfile() {
        ResponseEntity<Map> response = put("normal-user", """
                {
                  "id": 999,
                  "userId": "attacker-selected",
                  "skills": ["  Cafe\u0301  "],
                  "aspirations": {"targetRoles": ["  Lead Developer  "]},
                  "workPreferences": {
                    "location": {"postcode": " sw1a1aa ", "region": " London "},
                    "commuteRange": 25
                  },
                  "qualifications": [],
                  "roles": []
                }
                """);

        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        assertEquals("normal-user", response.getBody().get("userId"));
        assertNotEquals(999, response.getBody().get("id"));
        assertEquals("Café", ((java.util.List<?>) response.getBody().get("skills")).get(0));
        Map<?, ?> preferences = (Map<?, ?>) response.getBody().get("workPreferences");
        Map<?, ?> location = (Map<?, ?>) preferences.get("location");
        assertEquals("SW1A 1AA", location.get("postcode"));
        assertEquals("London", location.get("region"));
    }

    @Test
    void rejectsOversizedAndBlankSkillElementsWithStableFields() {
        String tooManySkills = IntStream.range(0, 101)
                .mapToObj(index -> "\"skill-" + index + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        ResponseEntity<Map> countResponse = put("count-user", "{\"skills\":[" + tooManySkills + "]}");
        assertValidationFailure(countResponse, "skills", "SIZE");

        ResponseEntity<Map> blankResponse = put("blank-user", "{\"skills\":[\"   \"]}");
        assertValidationFailure(blankResponse, "skills[0]", "NOTBLANK");
    }

    @Test
    void rejectsInvalidNestedLocationAndCommuteBounds() {
        ResponseEntity<Map> postcodeResponse = put("postcode-user", """
                {"workPreferences":{"location":{"postcode":"not a postcode"},"commuteRange":25}}
                """);
        assertValidationFailure(postcodeResponse, "workPreferences.location.postcode", "PATTERN");

        ResponseEntity<Map> commuteResponse = put("commute-user", """
                {"workPreferences":{"commuteRange":501}}
                """);
        assertValidationFailure(commuteResponse, "workPreferences.commuteRange", "MAX");
    }

    @Test
    void acceptsNumericAndOutcodeBoundaries() {
        ResponseEntity<Map> response = put("boundary-user", """
                {"workPreferences":{"location":{
                  "postcode":"ec1a","latitude":90,"longitude":-180
                },"commuteRange":500},
                "roles":[{"jobTitle":"Engineer","employer":"Employer","status":"PREVIOUS_ROLE",
                  "startDate":"2020-01-15","endDate":"2025-12-31"}]}
                """);

        assertEquals(HttpStatus.OK, response.getStatusCode(), () -> String.valueOf(response.getBody()));
        Map<?, ?> preferences = (Map<?, ?>) response.getBody().get("workPreferences");
        Map<?, ?> location = (Map<?, ?>) preferences.get("location");
        assertEquals("EC1A", location.get("postcode"));
        assertEquals(500, preferences.get("commuteRange"));
    }

    @Test
    void rejectsNullNestedEntriesAndOversizedStrings() {
        ResponseEntity<Map> nullResponse = put("null-user", "{\"qualifications\":[null]}");
        assertValidationFailure(nullResponse, "qualifications[0]", "NOTNULL");

        ResponseEntity<Map> longResponse = put("long-user", "{\"skills\":[\"" + "x".repeat(101) + "\"]}");
        assertValidationFailure(longResponse, "skills[0]", "SIZE");
    }

    @Test
    void rejectsInvalidConditionalQualificationWithoutLeakingInput() {
        String body = """
                {"qualifications":[{
                  "qualificationName":"Sensitive qualification",
                  "issuingBody":"Sensitive issuer",
                  "status":"COMPLETED",
                  "dateAchieved":"2026-01"
                }]}
                """;
        ResponseEntity<Map> response = put("qualification-user", body);

        assertValidationFailure(response, "qualifications.grade", "REQUIRED_FOR_STATUS");
        assertFalse(response.getBody().toString().contains("Sensitive qualification"));
        assertFalse(response.getBody().toString().contains("Sensitive issuer"));
    }

    @Test
    void rejectsInvalidNestedRoleFieldsAndDates() {
        ResponseEntity<Map> missingTitle = put("role-title-user", """
                {"roles":[{"jobTitle":" ","employer":"Employer","status":"CURRENT","startDate":"2026-01"}]}
                """);
        assertValidationFailure(missingTitle, "roles[0].jobTitle", "NOTBLANK");

        ResponseEntity<Map> invalidMonth = put("role-date-user", """
                {"roles":[{"jobTitle":"Engineer","employer":"Employer","status":"CURRENT","startDate":"2026-13"}]}
                """);
        assertValidationFailure(invalidMonth, "roles[0].startDate", "PROFILEDATE");

        ResponseEntity<Map> invalidDay = put("role-day-user", """
                {"roles":[{"jobTitle":"Engineer","employer":"Employer","status":"CURRENT","startDate":"2026-02-30"}]}
                """);
        assertValidationFailure(invalidDay, "roles[0].startDate", "PROFILEDATE");
    }

    @Test
    void rejectsMalformedJsonAndUnsupportedContentTypeSafely() {
        ResponseEntity<Map> malformed = put("json-user", "{not-json");
        assertEquals(HttpStatus.BAD_REQUEST, malformed.getStatusCode());
        assertEquals("MALFORMED_JSON", malformed.getBody().get("code"));
        assertFalse(malformed.getBody().toString().contains("not-json"));

        HttpHeaders headers = headers("media-user");
        headers.setContentType(MediaType.TEXT_PLAIN);
        ResponseEntity<Map> media = restTemplate.exchange(
                "/api/profiles/me", HttpMethod.PUT, new HttpEntity<>("{}", headers), Map.class);
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, media.getStatusCode());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", media.getBody().get("code"));
    }

    @Test
    void rejectsPayloadAboveConfiguredLimit() {
        ResponseEntity<Map> response = put("large-user", "{\"skills\":[\"" + "x".repeat(70_000) + "\"]}");

        assertEquals(413, response.getStatusCode().value());
        assertEquals("PAYLOAD_TOO_LARGE", response.getBody().get("code"));
        assertFalse(response.getBody().toString().contains("x".repeat(100)));
    }

    private ResponseEntity<Map> put(String userId, String body) {
        return restTemplate.exchange("/api/profiles/me", HttpMethod.PUT,
                new HttpEntity<>(body, headers(userId)), Map.class);
    }

    private HttpHeaders headers(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId);
        headers.set("X-Correlation-Id", "profile-03-" + userId);
        return headers;
    }

    private void assertValidationFailure(ResponseEntity<Map> response, String field, String code) {
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("1", response.getBody().get("schemaVersion"));
        assertEquals("PROFILE_VALIDATION_FAILED", response.getBody().get("code"));
        assertTrue(((java.util.List<?>) response.getBody().get("violations")).stream()
                .map(value -> (Map<?, ?>) value)
                .anyMatch(value -> field.equals(value.get("field")) && code.equals(value.get("code"))),
                () -> "Missing expected violation in " + response.getBody());
        assertNotNull(response.getBody().get("correlationId"));
        assertNotNull(response.getBody().get("timestamp"));
    }
}
