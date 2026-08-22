package com.jobseekercopilot.userprofileservice.model;

import com.jobseekercopilot.userprofileservice.validation.ProfileConstraints;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostcodeLocation {

    @Column(name = "location_id", length = 36)
    @Size(max = 36)
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$")
    private String locationId;

    @Column(name = "location_display_name", length = 200)
    @Size(max = 200)
    private String displayName;

    @Column(name = "location_country_code", length = 2)
    @Size(min = 2, max = 2)
    @Pattern(regexp = "^[A-Z]{2}$")
    private String countryCode;

    @Column(length = 16)
    @Size(max = 16)
    @Pattern(regexp = ProfileConstraints.POSTCODE_PATTERN)
    private String postcode;

    @Column(length = 100)
    @Size(max = 100)
    private String region;

    @Column(name = "admin_district", length = 100)
    @Size(max = 100)
    private String adminDistrict;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 24)
    private CanonicalLocationType locationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_precision", length = 32)
    private LocationPrecision precision;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_confidence", length = 16)
    private LocationConfidence confidence;

    @Column(name = "google_place_id", length = 255)
    @Size(max = 255)
    private String googlePlaceId;

    @Column(name = "postcodes_io_place_id", length = 128)
    @Size(max = 128)
    private String postcodesIoPlaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_name_source", length = 24)
    private LocationSource displayNameSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "postcode_source", length = 24)
    private LocationSource postcodeSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "coordinates_source", length = 24)
    private LocationSource coordinatesSource;
}
