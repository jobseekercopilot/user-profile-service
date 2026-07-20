package com.jobseekercopilot.userprofileservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobseekercopilot.userprofileservice.validation.ProfileConstraints;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(unique = true, nullable = false, length = 128)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String userId;

    @Size(max = ProfileConstraints.MAX_SKILLS)
    @ElementCollection
    @CollectionTable(name = "user_profile_skills", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "skill", length = 100)
    private List<@NotBlank @Size(max = 100) String> skills = new ArrayList<>();

    @Valid
    @Embedded
    private Aspirations aspirations;

    @Valid
    @Embedded
    private WorkPreferences workPreferences;

    @Size(max = ProfileConstraints.MAX_QUALIFICATIONS)
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<@NotNull @Valid Qualification> qualifications = new ArrayList<>();

    @Size(max = ProfileConstraints.MAX_ROLES)
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<@NotNull @Valid Role> roles = new ArrayList<>();
}
