package com.jobseekercopilot.userprofileservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
    private Long id;

    @Column(nullable = false)
    private String jobTitle;

    @Column(nullable = false)
    private String employer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleStatus status;

    @Column(nullable = false)
    private String startDate;

    @Column
    private String endDate;

    @Column(length = 2000)
    private String keyResponsibilities;

    @ManyToOne
    @JoinColumn(name = "user_profile_id", nullable = false)
    @JsonIgnore
    private UserProfile userProfile;

}
