package com.jobseekercopilot.userprofileservice.model;

import com.jobseekercopilot.userprofileservice.validation.ProfileConstraints;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
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
public class Aspirations {

    @ElementCollection
    @Size(max = ProfileConstraints.MAX_TARGET_ROLES)
    @Column(length = 100)
    private List<@NotBlank @Size(max = 100) String> targetRoles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private TargetWeeklyHours targetWeeklyHours;
}
