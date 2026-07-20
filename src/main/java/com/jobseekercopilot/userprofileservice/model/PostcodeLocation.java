package com.jobseekercopilot.userprofileservice.model;

import com.jobseekercopilot.userprofileservice.validation.ProfileConstraints;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
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

    @Column(length = 8)
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
}
