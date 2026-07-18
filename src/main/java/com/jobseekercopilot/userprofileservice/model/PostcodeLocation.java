package com.jobseekercopilot.userprofileservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
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

    private String postcode;

    private String region;

    @Column(name = "admin_district")
    private String adminDistrict;

    private Double latitude;

    private Double longitude;
}