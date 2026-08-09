package com.jobseekercopilot.userprofileservice.validation;

import com.jobseekercopilot.userprofileservice.model.Aspirations;
import com.jobseekercopilot.userprofileservice.model.PostcodeLocation;
import com.jobseekercopilot.userprofileservice.model.Qualification;
import com.jobseekercopilot.userprofileservice.model.Role;
import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.WorkPreferences;
import com.jobseekercopilot.userprofileservice.model.CanonicalLocationType;
import com.jobseekercopilot.userprofileservice.model.LocationConfidence;
import com.jobseekercopilot.userprofileservice.model.LocationPrecision;
import com.jobseekercopilot.userprofileservice.model.LocationSource;
import com.jobseekercopilot.userprofileservice.exception.ProfileValidationException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.UUID;

@Component
public class ProfileNormalizer {

    public void normalize(UserProfile profile) {
        profile.setSkills(normalizeList(profile.getSkills()));
        normalize(profile.getAspirations());
        normalize(profile.getWorkPreferences());
        if (profile.getQualifications() != null) {
            profile.getQualifications().forEach(this::normalize);
        }
        if (profile.getRoles() != null) {
            profile.getRoles().forEach(this::normalize);
        }
    }

    private void normalize(Aspirations aspirations) {
        if (aspirations != null) {
            aspirations.setTargetRoles(normalizeList(aspirations.getTargetRoles()));
        }
    }

    private void normalize(WorkPreferences preferences) {
        if (preferences == null || preferences.getLocation() == null) {
            if (preferences != null) {
                normalisePreferenceCollections(preferences);
            }
            return;
        }
        normalisePreferenceCollections(preferences);
        PostcodeLocation location = preferences.getLocation();
        location.setPostcode(normalizePostcode(location.getPostcode()));
        location.setRegion(normalizeText(location.getRegion()));
        location.setAdminDistrict(normalizeText(location.getAdminDistrict()));
        location.setDisplayName(normalizeText(location.getDisplayName()));
        location.setCountryCode(location.getCountryCode() == null
                ? null : location.getCountryCode().strip().toUpperCase(Locale.ROOT));
        location.setGooglePlaceId(normalizeText(location.getGooglePlaceId()));
        location.setPostcodesIoPlaceId(normalizeText(location.getPostcodesIoPlaceId()));
        rejectRestrictedGoogleContent(location);
        applyLegacyCanonicalDefaults(location);
    }

    private void rejectRestrictedGoogleContent(PostcodeLocation location) {
        if (location.getDisplayNameSource() == LocationSource.GOOGLE_PLACES
                || location.getPostcodeSource() == LocationSource.GOOGLE_PLACES
                || location.getCoordinatesSource() == LocationSource.GOOGLE_PLACES) {
            throw new ProfileValidationException(
                    "workPreferences.location",
                    "RESTRICTED_PROVIDER_CONTENT",
                    "Google Maps place content cannot be stored as canonical profile data");
        }
    }

    private void normalisePreferenceCollections(WorkPreferences preferences) {
        preferences.setEmploymentTypes(preferences.getEmploymentTypes() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(preferences.getEmploymentTypes()));
        preferences.setWorkingPatterns(preferences.getWorkingPatterns() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(preferences.getWorkingPatterns()));
        preferences.setWorkplaceArrangements(preferences.getWorkplaceArrangements() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(preferences.getWorkplaceArrangements()));
        preferences.setCommuteTravelModes(preferences.getCommuteTravelModes() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(preferences.getCommuteTravelModes()));
    }

    private void applyLegacyCanonicalDefaults(PostcodeLocation location) {
        if (location.getPostcode() == null) {
            return;
        }
        if (location.getLocationId() == null) location.setLocationId(UUID.randomUUID().toString());
        if (location.getDisplayName() == null) {
            location.setDisplayName(location.getAdminDistrict() != null
                    ? location.getAdminDistrict() : location.getRegion());
        }
        if (location.getCountryCode() == null) location.setCountryCode("GB");
        if (location.getLocationType() == null) location.setLocationType(CanonicalLocationType.POSTCODE);
        if (location.getPrecision() == null) location.setPrecision(LocationPrecision.POSTCODE_CENTROID);
        if (location.getConfidence() == null) location.setConfidence(LocationConfidence.VERIFIED);
        if (location.getDisplayNameSource() == null) location.setDisplayNameSource(LocationSource.LEGACY);
        if (location.getPostcodeSource() == null) location.setPostcodeSource(LocationSource.LEGACY);
        if (location.getCoordinatesSource() == null
                && location.getLatitude() != null && location.getLongitude() != null) {
            location.setCoordinatesSource(LocationSource.LEGACY);
        }
    }

    private void normalize(Qualification qualification) {
        if (qualification == null) {
            return;
        }
        qualification.setQualificationName(normalizeText(qualification.getQualificationName()));
        qualification.setIssuingBody(normalizeText(qualification.getIssuingBody()));
        qualification.setGrade(normalizeText(qualification.getGrade()));
        qualification.setDateAchieved(normalizeText(qualification.getDateAchieved()));
        qualification.setExpectedCompletion(normalizeText(qualification.getExpectedCompletion()));
    }

    private void normalize(Role role) {
        if (role == null) {
            return;
        }
        role.setJobTitle(normalizeText(role.getJobTitle()));
        role.setEmployer(normalizeText(role.getEmployer()));
        role.setStartDate(normalizeText(role.getStartDate()));
        role.setEndDate(normalizeText(role.getEndDate()));
        role.setKeyResponsibilities(normalizeText(role.getKeyResponsibilities()));
    }

    private List<String> normalizeList(List<String> values) {
        return values == null
                ? new ArrayList<>()
                : values.stream().map(this::normalizeText)
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private String normalizePostcode(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        String compact = normalized.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return compact.length() > 4
                ? compact.substring(0, compact.length() - 3) + " " + compact.substring(compact.length() - 3)
                : compact;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
