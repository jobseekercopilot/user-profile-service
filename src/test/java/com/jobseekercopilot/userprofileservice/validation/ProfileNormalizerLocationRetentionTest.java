package com.jobseekercopilot.userprofileservice.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jobseekercopilot.userprofileservice.exception.ProfileValidationException;
import com.jobseekercopilot.userprofileservice.model.LocationSource;
import com.jobseekercopilot.userprofileservice.model.PostcodeLocation;
import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.WorkPreferences;
import org.junit.jupiter.api.Test;

class ProfileNormalizerLocationRetentionTest {

    private final ProfileNormalizer normalizer = new ProfileNormalizer();

    @Test
    void permitsGooglePlaceIdWhenCanonicalFieldsComeFromPostcodesIo() {
        PostcodeLocation location = location();
        location.setGooglePlaceId("ChIJ-test-place-id");
        location.setDisplayNameSource(LocationSource.POSTCODES_IO);
        location.setPostcodeSource(LocationSource.POSTCODES_IO);
        location.setCoordinatesSource(LocationSource.POSTCODES_IO);

        assertDoesNotThrow(() -> normalizer.normalize(profile(location)));
    }

    @Test
    void rejectsGoogleDerivedCanonicalFieldsBeforePersistence() {
        PostcodeLocation location = location();
        location.setCoordinatesSource(LocationSource.GOOGLE_PLACES);

        assertThrows(
                ProfileValidationException.class,
                () -> normalizer.normalize(profile(location)));
    }

    private UserProfile profile(PostcodeLocation location) {
        WorkPreferences preferences = new WorkPreferences();
        preferences.setLocation(location);
        UserProfile profile = new UserProfile();
        profile.setWorkPreferences(preferences);
        return profile;
    }

    private PostcodeLocation location() {
        PostcodeLocation location = new PostcodeLocation();
        location.setDisplayName("Westminster, London");
        location.setPostcode("SW1A 2AA");
        location.setLatitude(51.5034);
        location.setLongitude(-0.1276);
        return location;
    }
}
