package com.jobseekercopilot.userprofileservice.validation;

import com.jobseekercopilot.userprofileservice.exception.ProfileValidationException;
import com.jobseekercopilot.userprofileservice.model.ProfessionalContact;
import com.jobseekercopilot.userprofileservice.model.ProfessionalLink;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileNormalizerProfessionalContactTest {

    private final ProfileNormalizer normalizer = new ProfileNormalizer();

    @Test
    void normalizesExplicitSyntheticContactWithoutInferringValues() {
        ProfessionalContact contact = new ProfessionalContact();
        contact.setPhone("  +44 20 7946 0958  ");
        contact.setLinks(List.of(new ProfessionalLink(
                "  GitHub  ",
                "  https://github.com/example-developer  ")));

        normalizer.normalize(contact);

        assertEquals("+44 20 7946 0958", contact.getPhone());
        assertEquals("GitHub", contact.getLinks().get(0).getLabel());
        assertEquals(
                "https://github.com/example-developer",
                contact.getLinks().get(0).getUrl());
    }

    @Test
    void rejectsCredentialedAndDuplicateProfessionalLinks() {
        ProfessionalContact credentialed = new ProfessionalContact();
        credentialed.setLinks(List.of(new ProfessionalLink(
                "Private",
                "https://user:secret@example.test/profile")));
        ProfileValidationException unsafe = assertThrows(
                ProfileValidationException.class,
                () -> normalizer.normalize(credentialed));
        assertEquals("professionalContact.links[0].url", unsafe.getField());
        assertEquals("HTTPSURL", unsafe.getCode());

        ProfessionalContact duplicate = new ProfessionalContact();
        duplicate.setLinks(List.of(
                new ProfessionalLink(
                        "GitHub",
                        "https://github.com/example-one"),
                new ProfessionalLink(
                        " github ",
                        "https://github.com/example-two")));
        ProfileValidationException repeated = assertThrows(
                ProfileValidationException.class,
                () -> normalizer.normalize(duplicate));
        assertEquals("professionalContact.links[1].label", repeated.getField());
        assertEquals("DUPLICATE", repeated.getCode());
    }
}
