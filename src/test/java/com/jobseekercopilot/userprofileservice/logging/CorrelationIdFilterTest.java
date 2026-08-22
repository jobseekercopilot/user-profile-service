package com.jobseekercopilot.userprofileservice.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter("user-profile-service");

    @Test
    void acceptsSafeCorrelationIdAndClearsLoggingContext() throws Exception {
        var request = request("profile-safe-123");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertEquals("profile-safe-123", MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertEquals("profile-safe-123", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void replacesUnsafeCorrelationIdBeforeLoggingOrResponding() throws Exception {
        String unsafe = "unsafe correlation id";
        var request = request(unsafe);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertNotEquals(unsafe, MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertNotEquals(unsafe, response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    private MockHttpServletRequest request(String correlationId) {
        var request = new MockHttpServletRequest("GET", "/api/profiles/me");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, correlationId);
        return request;
    }
}
