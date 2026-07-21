package com.jobseekercopilot.userprofileservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.env.Environment;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class HealthReadinessIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private Environment environment;

    @Test
    void generalHealthAndDatabaseReadinessAreRedacted() {
        ResponseEntity<Map> health = restTemplate.getForEntity("/actuator/health", Map.class);
        ResponseEntity<Map> readiness = restTemplate.getForEntity("/actuator/health/readiness", Map.class);

        assertEquals(HttpStatus.OK, health.getStatusCode());
        assertEquals("UP", health.getBody().get("status"));
        assertFalse(health.getBody().containsKey("components"));

        assertEquals(HttpStatus.OK, readiness.getStatusCode());
        assertEquals("UP", readiness.getBody().get("status"));
        assertFalse(readiness.getBody().containsKey("components"));
        assertEquals("readinessState,db",
                environment.getProperty("management.endpoint.health.group.readiness.include"));
    }

    @Test
    void metricsEndpointIsNotPubliclyExposed() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                restTemplate.getForEntity("/actuator/metrics", Map.class).getStatusCode());
    }
}
