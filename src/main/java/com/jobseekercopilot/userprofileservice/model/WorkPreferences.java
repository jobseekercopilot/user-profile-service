package com.jobseekercopilot.userprofileservice.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
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

    @Embedded
    private PostcodeLocation location;

    private Integer commuteRange;
}
