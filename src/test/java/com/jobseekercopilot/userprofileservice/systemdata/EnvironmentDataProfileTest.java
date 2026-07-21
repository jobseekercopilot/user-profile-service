package com.jobseekercopilot.userprofileservice.systemdata;

import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import com.jobseekercopilot.userprofileservice.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EnvironmentDataProfileTest {
    @Test
    void controllerIsAbsentWithoutExplicitProfile() {
        try (AnnotationConfigApplicationContext context = context("local")) {
            assertFalse(context.containsBeanDefinition("userProfileSystemDataController"));
        }
    }

    @Test
    void controllerIsPresentWithExplicitProfile() {
        try (AnnotationConfigApplicationContext context = context("local", "environment-data")) {
            assertTrue(context.containsBeanDefinition("userProfileSystemDataController"));
        }
    }

    private AnnotationConfigApplicationContext context(String... profiles) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profiles);
        context.register(UserProfileSystemDataController.class, Dependencies.class);
        context.refresh();
        return context;
    }

    @Configuration(proxyBeanMethods = false)
    static class Dependencies {
        @Bean
        EnvironmentDataGuard environmentDataGuard() {
            return mock(EnvironmentDataGuard.class);
        }

        @Bean
        UserProfileService userProfileService() {
            return mock(UserProfileService.class);
        }

        @Bean
        UserProfileRepository userProfileRepository() {
            return mock(UserProfileRepository.class);
        }
    }
}
