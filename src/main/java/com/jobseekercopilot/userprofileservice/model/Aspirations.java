package com.jobseekercopilot.userprofileservice.model;

import com.jobseekercopilot.userprofileservice.validation.ProfileConstraints;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embeddable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ForeignKey;
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
    @CollectionTable(
            name = "user_profile_target_roles",
            joinColumns = @JoinColumn(name = "user_profile_id"),
            foreignKey = @ForeignKey(name = "fk_profile_target_roles_profile"))
    @Size(max = ProfileConstraints.MAX_TARGET_ROLES)
    @Column(name = "target_role", nullable = false, length = 100)
    private List<@NotBlank @Size(max = 100) String> targetRoles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "target_weekly_hours", length = 32)
    private TargetWeeklyHours targetWeeklyHours;
}
