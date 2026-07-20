package com.jobseekercopilot.userprofileservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobseekercopilot.userprofileservice.validation.ProfileDate;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Qualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(nullable = false, length = 200)
    @NotBlank
    @Size(max = 200)
    private String qualificationName;

    @Column(nullable = false, length = 200)
    @NotBlank
    @Size(max = 200)
    private String issuingBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private QualificationStatus status;

    @Column(length = 100)
    @Size(max = 100)
    private String grade;

    @Column(length = 10)
    @Size(max = 30)
    @ProfileDate
    private String dateAchieved;

    @Column(length = 10)
    @Size(max = 30)
    @ProfileDate
    private String expectedCompletion;

    @ManyToOne
    @JoinColumn(name = "user_profile_id", nullable = false)
    @JsonIgnore
    private UserProfile userProfile;

}
