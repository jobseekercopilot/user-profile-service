package com.jobseekercopilot.userprofileservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.repository.UserProfileRepository;
import com.jobseekercopilot.userprofileservice.service.UserProfileService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class ProfileUpsertConcurrencyIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("profile_concurrency")
                    .withUsername("profile_test")
                    .withPassword("test-only-database-password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearProfiles() {
        userProfileRepository.deleteAll();
    }

    @Test
    void concurrentDuplicateCreatesAreIdempotentAndRetriable() throws Exception {
        runConcurrently(8, index -> profile("Java"));

        assertEquals(1, userProfileRepository.count());
        assertEquals(List.of("Java"), storedSkills());

        userProfileService.createOrUpdateProfile("concurrent-user", profile("Java"));
        assertEquals(1, userProfileRepository.count());
        assertEquals(List.of("Java"), storedSkills());
    }

    @Test
    void concurrentDifferentUpdatesNeverProduceMixedOrDuplicateState() throws Exception {
        userProfileService.createOrUpdateProfile("concurrent-user", profile("Initial"));
        List<String> completePayloads = List.of("Java", "Kotlin", "Go", "Rust", "Python", "C#");

        runConcurrently(completePayloads.size(), index -> profile(completePayloads.get(index)));

        assertEquals(1, userProfileRepository.count());
        List<String> skills = storedSkills();
        assertEquals(1, skills.size());
        assertTrue(completePayloads.contains(skills.get(0)));
    }

    private void runConcurrently(int count, java.util.function.IntFunction<UserProfile> payload)
            throws Exception {
        var executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<UserProfile>> writes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int payloadIndex = index;
            writes.add(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent writers did not start together");
                }
                return userProfileService.createOrUpdateProfile("concurrent-user", payload.apply(payloadIndex));
            });
        }

        try {
            List<Future<UserProfile>> results = writes.stream().map(executor::submit).toList();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (Future<UserProfile> result : results) {
                result.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private UserProfile profile(String skill) {
        UserProfile profile = new UserProfile();
        profile.setSkills(List.of(skill));
        return profile;
    }

    private List<String> storedSkills() {
        return jdbcTemplate.queryForList("SELECT skill FROM user_profile_skills", String.class);
    }
}
