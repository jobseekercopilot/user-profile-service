package com.jobseekercopilot.userprofileservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.jobseekercopilot.userprofileservice.model.Aspirations;
import com.jobseekercopilot.userprofileservice.model.UserProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfileDigestCalculatorTest {

    private final ProfileDigestCalculator calculator = new ProfileDigestCalculator();

    @Test
    void digestIsStableAndExcludesServerDatabaseIdentity() {
        UserProfile first = profile("Developer");
        first.setId(1L);
        first.setUserId("owner-one");
        UserProfile second = profile("Developer");
        second.setId(99L);
        second.setUserId("owner-two");

        assertEquals(calculator.digest(first), calculator.digest(second));
        assertEquals(64, calculator.digest(first).length());
    }

    @Test
    void digestChangesWhenUserDeclaredContentChanges() {
        assertNotEquals(
                calculator.digest(profile("Developer")),
                calculator.digest(profile("Analyst")));
    }

    private UserProfile profile(String role) {
        UserProfile profile = new UserProfile();
        profile.setSkills(List.of("Java"));
        Aspirations aspirations = new Aspirations();
        aspirations.setTargetRoles(List.of(role));
        profile.setAspirations(aspirations);
        return profile;
    }
}
