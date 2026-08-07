package com.jobseekercopilot.userprofileservice.security;

import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ProfileSecurityConfig {

    @Bean
    SecurityFilterChain profileSecurityFilterChain(
            HttpSecurity http, ProfileAuthenticationEntryPoint authenticationEntryPoint)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/internal/system-data/**").permitAll()
                        .requestMatchers("/internal/account-lifecycle/**")
                        .hasAuthority(ProfileAuthorities.ACCOUNT_LIFECYCLE)
                        .requestMatchers("/api/profiles/**", "/api/evidence/**")
                        .hasAuthority(ProfileAuthorities.USER)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .hasAuthority(ProfileAuthorities.USER)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(authenticationEntryPoint))
                .build();
    }

    @Bean
    JwtDecoder profileJwtDecoder(
            @Value("${profile.security.jwk-set-uri}") String jwkSetUri,
            @Value("${profile.security.issuer}") String issuer,
            @Value("${profile.security.audience}") String audience) {
        validateConfiguration(jwkSetUri, issuer, audience);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                requiredAudience(audience),
                requiredSupportedToken()));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String tokenType = jwt.getClaimAsString("token_type");
            if ("access".equals(tokenType)) {
                return List.of(new SimpleGrantedAuthority(ProfileAuthorities.USER));
            }
            if (isLifecycleToken(jwt)) {
                return List.of(new SimpleGrantedAuthority(
                        ProfileAuthorities.ACCOUNT_LIFECYCLE));
            }
            return List.of();
        });
        return converter;
    }

    private static OAuth2TokenValidator<Jwt> requiredAudience(String audience) {
        return token -> token.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : invalidToken();
    }

    private static OAuth2TokenValidator<Jwt> requiredSupportedToken() {
        return token -> token.getSubject() != null && !token.getSubject().isBlank()
                        && ("access".equals(token.getClaimAsString("token_type"))
                        || isLifecycleToken(token))
                ? OAuth2TokenValidatorResult.success()
                : invalidToken();
    }

    private static boolean isLifecycleToken(Jwt token) {
        String operationId = token.getClaimAsString("operation_id");
        return "account_lifecycle".equals(token.getClaimAsString("token_type"))
                && operationId != null
                && !operationId.isBlank();
    }

    private static OAuth2TokenValidatorResult invalidToken() {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Access token validation failed.", null));
    }

    static void validateConfiguration(String jwkSetUri, String issuer, String audience) {
        try {
            URI uri = URI.create(jwkSetUri);
            if (!(List.of("http", "https").contains(uri.getScheme()))
                    || !uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getFragment() != null || issuer == null || issuer.isBlank()
                    || audience == null || audience.isBlank()) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Profile JWT verification configuration is invalid");
        }
    }
}
