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
public class Qualification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String qualificationName;

    @Column(nullable = false)
    private String issuingBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QualificationStatus status;

    @Column
    private String grade;

    @Column
    private String dateAchieved;

    @Column
    private String expectedCompletion;

    @ManyToOne
    @JoinColumn(name = "user_profile_id", nullable = false)
    @JsonIgnore
    private UserProfile userProfile;

}
