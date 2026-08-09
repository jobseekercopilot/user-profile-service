package com.jobseekercopilot.userprofileservice.service;

import com.jobseekercopilot.userprofileservice.model.Aspirations;
import com.jobseekercopilot.userprofileservice.model.PostcodeLocation;
import com.jobseekercopilot.userprofileservice.model.Qualification;
import com.jobseekercopilot.userprofileservice.model.Role;
import com.jobseekercopilot.userprofileservice.model.UserProfile;
import com.jobseekercopilot.userprofileservice.model.WorkPreferences;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProfileDigestCalculator {

    public String digest(UserProfile profile) {
        StringBuilder canonical = new StringBuilder("profile-v2|");
        appendList(canonical, profile.getSkills());

        Aspirations aspirations = profile.getAspirations();
        if (aspirations == null) {
            DigestSupport.append(canonical, null);
        } else {
            appendList(canonical, aspirations.getTargetRoles());
            DigestSupport.append(canonical, aspirations.getTargetWeeklyHours());
        }

        WorkPreferences preferences = profile.getWorkPreferences();
        if (preferences == null) {
            DigestSupport.append(canonical, null);
        } else {
            DigestSupport.append(canonical, preferences.getCommuteRange());
            appendSorted(canonical, preferences.getCommuteTravelModes());
            DigestSupport.append(canonical, preferences.getMaximumDrivingMinutes());
            DigestSupport.append(canonical, preferences.getMaximumTransitMinutes());
            appendSorted(canonical, preferences.getEmploymentTypes());
            appendSorted(canonical, preferences.getWorkingPatterns());
            appendSorted(canonical, preferences.getWorkplaceArrangements());
            DigestSupport.append(canonical, preferences.getAvailableFrom());
            DigestSupport.append(canonical, preferences.getNoticePeriodDays());
            PostcodeLocation location = preferences.getLocation();
            if (location == null) {
                DigestSupport.append(canonical, null);
            } else {
                DigestSupport.append(canonical, location.getPostcode());
                DigestSupport.append(canonical, location.getRegion());
                DigestSupport.append(canonical, location.getAdminDistrict());
                DigestSupport.append(canonical, location.getLatitude());
                DigestSupport.append(canonical, location.getLongitude());
                DigestSupport.append(canonical, location.getLocationId());
                DigestSupport.append(canonical, location.getDisplayName());
                DigestSupport.append(canonical, location.getCountryCode());
                DigestSupport.append(canonical, location.getLocationType());
                DigestSupport.append(canonical, location.getPrecision());
                DigestSupport.append(canonical, location.getConfidence());
                DigestSupport.append(canonical, location.getGooglePlaceId());
                DigestSupport.append(canonical, location.getPostcodesIoPlaceId());
                DigestSupport.append(canonical, location.getDisplayNameSource());
                DigestSupport.append(canonical, location.getPostcodeSource());
                DigestSupport.append(canonical, location.getCoordinatesSource());
            }
        }

        List<Qualification> qualifications = profile.getQualifications();
        canonical.append(qualifications == null ? -1 : qualifications.size()).append('|');
        if (qualifications != null) {
            for (Qualification qualification : qualifications) {
                DigestSupport.append(canonical, qualification.getQualificationName());
                DigestSupport.append(canonical, qualification.getIssuingBody());
                DigestSupport.append(canonical, qualification.getStatus());
                DigestSupport.append(canonical, qualification.getGrade());
                DigestSupport.append(canonical, qualification.getDateAchieved());
                DigestSupport.append(canonical, qualification.getExpectedCompletion());
            }
        }

        List<Role> roles = profile.getRoles();
        canonical.append(roles == null ? -1 : roles.size()).append('|');
        if (roles != null) {
            for (Role role : roles) {
                DigestSupport.append(canonical, role.getJobTitle());
                DigestSupport.append(canonical, role.getEmployer());
                DigestSupport.append(canonical, role.getStatus());
                DigestSupport.append(canonical, role.getStartDate());
                DigestSupport.append(canonical, role.getEndDate());
                DigestSupport.append(canonical, role.getKeyResponsibilities());
            }
        }
        return DigestSupport.sha256(canonical.toString());
    }

    private void appendList(StringBuilder canonical, List<String> values) {
        canonical.append(values == null ? -1 : values.size()).append('|');
        if (values != null) {
            values.forEach(value -> DigestSupport.append(canonical, value));
        }
    }

    private void appendSorted(StringBuilder canonical, java.util.Set<? extends Enum<?>> values) {
        if (values == null) {
            canonical.append(-1).append('|');
            return;
        }
        List<String> sorted = values.stream().map(Enum::name).sorted().toList();
        appendList(canonical, sorted);
    }
}
