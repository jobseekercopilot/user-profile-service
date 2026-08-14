package com.jobseekercopilot.userprofileservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.jobseekercopilot.userprofileservice.model.Aspirations;
import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.ProfessionalContact;
import com.jobseekercopilot.userprofileservice.model.ProfessionalLink;
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

    @Test
    void digestIncludesProfessionalContactButNotItsDatabaseIdentity() {
        UserProfile first = profile("Developer");
        ProfessionalContact firstContact = new ProfessionalContact();
        firstContact.setId(10L);
        firstContact.setPhone("+44 20 7946 0958");
        firstContact.setLinks(List.of(new ProfessionalLink(
                "GitHub",
                "https://github.com/example-developer")));
        first.setProfessionalContact(firstContact);

        UserProfile same = profile("Developer");
        ProfessionalContact sameContact = new ProfessionalContact();
        sameContact.setId(20L);
        sameContact.setPhone("+44 20 7946 0958");
        sameContact.setLinks(List.of(new ProfessionalLink(
                "GitHub",
                "https://github.com/example-developer")));
        same.setProfessionalContact(sameContact);

        assertEquals(calculator.digest(first), calculator.digest(same));
        sameContact.setPhone("+44 20 7946 0959");
        assertNotEquals(calculator.digest(first), calculator.digest(same));
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
