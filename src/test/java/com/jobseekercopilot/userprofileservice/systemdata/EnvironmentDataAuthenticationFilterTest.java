package com.jobseekercopilot.userprofileservice.systemdata;

import com.jobseekercopilot.userprofileservice.config.EnvironmentDataProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentDataAuthenticationFilterTest {
    private static final String VALID_TOKEN = "profile-environment-data-token-123456789";

    private EnvironmentDataAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        EnvironmentDataProperties properties = new EnvironmentDataProperties();
        properties.setEnabled(true);
        properties.setToken(VALID_TOKEN);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test", "environment-data");
        filter = new EnvironmentDataAuthenticationFilter(
                new EnvironmentDataGuard(properties, environment), JsonMapper.shared());
    }

    @Test
    void validTokenReachesInternalEndpoint() throws Exception {
        Result result = invoke("/internal/system-data/verify/profiles/user-1", VALID_TOKEN);

        assertTrue(result.chainInvoked());
        assertEquals(200, result.response().getStatus());
    }

    @Test
    void missingTokenIsDeniedWithoutLeakingCredential() throws Exception {
        Result result = invoke("/internal/system-data/verify/profiles/user-1");

        assertFalse(result.chainInvoked());
        assertEquals(401, result.response().getStatus());
        assertTrue(result.response().getContentAsString().contains("ENVIRONMENT_DATA_UNAUTHORIZED"));
        assertFalse(result.response().getContentAsString().contains(VALID_TOKEN));
    }

    @Test
    void invalidAndDuplicateTokensAreDenied() throws Exception {
        Result invalid = invoke("/internal/system-data/scenario/demo/profiles/user-1", "invalid-token");
        Result duplicate = invoke("/internal/system-data/seed/profiles/user-1", VALID_TOKEN, VALID_TOKEN);

        assertFalse(invalid.chainInvoked());
        assertEquals(401, invalid.response().getStatus());
        assertFalse(duplicate.chainInvoked());
        assertEquals(401, duplicate.response().getStatus());
    }

    @Test
    void unrelatedEndpointIsNotAffected() throws Exception {
        Result result = invoke("/api/profiles/me");

        assertTrue(result.chainInvoked());
    }

    private Result invoke(String path, String... tokens) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        for (String token : tokens) {
            request.addHeader(EnvironmentDataGuard.TOKEN_HEADER, token);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();
        FilterChain chain = (ignoredRequest, ignoredResponse) -> invoked.set(true);

        filter.doFilter(request, response, chain);
        return new Result(response, invoked.get());
    }

    private record Result(MockHttpServletResponse response, boolean chainInvoked) {
    }
}
