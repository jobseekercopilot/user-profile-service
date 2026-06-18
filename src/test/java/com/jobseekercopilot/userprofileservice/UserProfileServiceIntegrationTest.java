package com.jobseekercopilot.userprofileservice;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserProfileServiceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createAndGetProfile_HappyPath() {
        String userId = "integration-test-user-123";
        UserProfile profile = new UserProfile(null, userId, "Java, Spring", "5 years experience", "Team Lead", "Remote");

        // PUT profile
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId);
        HttpEntity<UserProfile> requestEntity = new HttpEntity<>(profile, headers);

        ResponseEntity<UserProfile> putResponse = restTemplate.exchange(
                "/api/profiles/me",
                HttpMethod.PUT,
                requestEntity,
                UserProfile.class);

        assertEquals(HttpStatus.OK, putResponse.getStatusCode());
        assertNotNull(putResponse.getBody());
        assertNotNull(putResponse.getBody().getId());
        assertEquals("Java, Spring", putResponse.getBody().getSkills());

        // GET profile
        ResponseEntity<UserProfile> getResponse = restTemplate.exchange(
                "/api/profiles/me",
                HttpMethod.GET,
                requestEntity,
                UserProfile.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals("Java, Spring", getResponse.getBody().getSkills());
        assertEquals("Remote", getResponse.getBody().getWorkPrefs());
    }
}