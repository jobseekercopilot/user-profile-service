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
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(nullable = false, length = 200)
    @NotBlank
    @Size(max = 200)
    private String jobTitle;

    @Column(nullable = false, length = 200)
    @NotBlank
    @Size(max = 200)
    private String employer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull
    private RoleStatus status;

    @Column(nullable = false, length = 10)
    @NotBlank
    @Size(max = 30)
    @ProfileDate
    private String startDate;

    @Column(length = 10)
    @Size(max = 30)
    @ProfileDate
    private String endDate;

    @Column(length = 2000)
    @Size(max = 2000)
    private String keyResponsibilities;

    @ManyToOne
    @JoinColumn(name = "user_profile_id", nullable = false)
    @JsonIgnore
    private UserProfile userProfile;

}
