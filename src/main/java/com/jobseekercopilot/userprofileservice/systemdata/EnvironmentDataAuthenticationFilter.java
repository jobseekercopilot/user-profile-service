package com.jobseekercopilot.userprofileservice.systemdata;

import com.jobseekercopilot.userprofileservice.exception.ErrorResponse;
import com.jobseekercopilot.userprofileservice.logging.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile(EnvironmentDataGuard.PROFILE)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class EnvironmentDataAuthenticationFilter extends OncePerRequestFilter {
    private static final String PATH = "/internal/system-data";

    private final EnvironmentDataGuard guard;
    private final ObjectMapper objectMapper;

    public EnvironmentDataAuthenticationFilter(EnvironmentDataGuard guard, ObjectMapper objectMapper) {
        this.guard = guard;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !(path.equals(PATH) || path.startsWith(PATH + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        List<String> tokens = Collections.list(request.getHeaders(EnvironmentDataGuard.TOKEN_HEADER));
        if (tokens.size() != 1 || !guard.hasValidToken(tokens.get(0))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                    "ENVIRONMENT_DATA_UNAUTHORIZED",
                    "Environment data credentials are invalid.",
                    MDC.get(CorrelationIdFilter.MDC_KEY)));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
