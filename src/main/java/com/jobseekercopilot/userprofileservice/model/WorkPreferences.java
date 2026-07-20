package com.jobseekercopilot.userprofileservice.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkPreferences {

    @Valid
    @Embedded
    private PostcodeLocation location;

    @Min(0)
    @Max(500)
    private Integer commuteRange;
}
