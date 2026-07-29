package com.jobseekercopilot.userprofileservice.model.evidence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EvidenceWriteRequest {

    @NotNull
    private EvidenceCategory category;

    @NotBlank
    @Size(max = 200)
    private String heading;

    @Size(max = 200)
    private String organisationContext;

    @Size(max = 200)
    private String roleTitle;

    @Size(max = 200)
    private String programmeOrSubject;

    @Size(max = 200)
    private String institution;

    @Size(max = 200)
    private String qualificationTitle;

    @Size(max = 200)
    private String issuer;

    @Size(max = 100)
    private String resultOrStatus;

    @Size(max = 200)
    private String projectRole;

    @Size(max = 4000)
    private String description;

    @Size(max = 4000)
    private String responsibilities;

    @Size(max = 4000)
    private String achievements;

    @Size(max = 1000)
    private String careerBreakReason;

    @Size(max = 500)
    private String privateCredentialIdentifier;

    private boolean ongoing;

    @Valid
    private PartialDate startDate;

    @Valid
    private PartialDate endDate;

    @Valid
    private PartialDate issueDate;

    @Valid
    private PartialDate expiryDate;

    @Size(max = 50)
    private List<@NotBlank @Size(max = 100) String> demonstratedSkills = new ArrayList<>();

    @Size(max = 10)
    private List<@NotBlank @Size(max = 2048) String> supportingLinks = new ArrayList<>();
}
