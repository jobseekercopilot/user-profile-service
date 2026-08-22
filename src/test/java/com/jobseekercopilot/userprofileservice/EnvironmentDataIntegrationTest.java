package com.jobseekercopilot.userprofileservice;

import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import com.jobseekercopilot.userprofileservice.systemdata.EnvironmentDataGuard;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "environment-data.enabled=true",
        "environment-data.token=profile-environment-data-integration-token"
})
@AutoConfigureTestRestTemplate
@ActiveProfiles({"test", "environment-data"})
class EnvironmentDataIntegrationTest {
    private static final String TOKEN = "profile-environment-data-integration-token";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserProfileRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void deniesMissingInvalidAndDuplicateCredentials() {
        ResponseEntity<Map> missing = request(HttpMethod.GET, "/internal/system-data/verify/profiles/test-user", null);
        ResponseEntity<Map> invalid = request(HttpMethod.GET, "/internal/system-data/verify/profiles/test-user", "invalid-token");

        HttpHeaders duplicates = new HttpHeaders();
        duplicates.add(EnvironmentDataGuard.TOKEN_HEADER, TOKEN);
        duplicates.add(EnvironmentDataGuard.TOKEN_HEADER, TOKEN);
        ResponseEntity<Map> duplicate = restTemplate.exchange(
                "/internal/system-data/verify/profiles/test-user", HttpMethod.GET,
                new HttpEntity<>(duplicates), Map.class);

        for (ResponseEntity<Map> response : java.util.List.of(missing, invalid, duplicate)) {
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("ENVIRONMENT_DATA_UNAUTHORIZED", response.getBody().get("code"));
            assertFalse(response.getBody().toString().contains(TOKEN));
        }
    }

    @Test
    void validIndependentCredentialAllowsFixtureLifecycle() {
        HttpHeaders headers = headers(TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> seeded = restTemplate.exchange(
                "/internal/system-data/seed/profiles/test-user", HttpMethod.POST,
                new HttpEntity<>("{\"skills\":[\"Java\"]}", headers), Map.class);
        ResponseEntity<Map> verified = request(
                HttpMethod.GET, "/internal/system-data/verify/profiles/test-user", TOKEN);
        ResponseEntity<Map> reset = request(
                HttpMethod.DELETE, "/internal/system-data/scenario/test-scenario/profiles/test-user", TOKEN);

        assertEquals(HttpStatus.OK, seeded.getStatusCode());
        assertEquals("test", seeded.getBody().get("activeEnvironment"));
        assertEquals(HttpStatus.OK, verified.getStatusCode());
        assertTrue((Boolean) ((Map<?, ?>) verified.getBody().get("details")).get("exists"));
        assertEquals(HttpStatus.OK, reset.getStatusCode());
        assertFalse(repository.findByUserId("test-user").isPresent());
    }

    private ResponseEntity<Map> request(HttpMethod method, String path, String token) {
        return restTemplate.exchange(path, method, new HttpEntity<>(headers(token)), Map.class);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set(EnvironmentDataGuard.TOKEN_HEADER, token);
        }
        return headers;
    }
}
