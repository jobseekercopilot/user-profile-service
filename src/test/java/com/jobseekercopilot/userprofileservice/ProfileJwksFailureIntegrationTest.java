package com.jobseekercopilot.userprofileservice;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "profile.security.jwk-set-uri=http://127.0.0.1:1/.well-known/jwks.json"
})
@AutoConfigureTestRestTemplate
class ProfileJwksFailureIntegrationTest {

    private static final TestJwksServer TOKEN_ISSUER = new TestJwksServer();

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void unavailableJwksFailsClosedWithoutProviderOrTokenDetails() {
        String token = TOKEN_ISSUER.validToken("provider-failure-subject");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-Correlation-Id", "jwks-provider-failure");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/profiles/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("PROFILE_AUTHENTICATION_REQUIRED", response.getBody().get("code"));
        assertEquals("jwks-provider-failure", response.getBody().get("correlationId"));
        assertFalse(response.getBody().toString().contains(token));
        assertFalse(response.getBody().toString().contains("127.0.0.1"));
        assertFalse(response.getBody().toString().contains("provider-failure-subject"));
    }
}
