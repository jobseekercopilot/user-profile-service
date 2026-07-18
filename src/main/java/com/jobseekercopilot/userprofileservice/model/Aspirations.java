package com.jobseekercopilot.userprofileservice.model;

import com.jobseekercopilot.userprofileservice.model.TargetWeeklyHours;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    private List<String> targetRoles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private TargetWeeklyHours targetWeeklyHours;
}
