package com.jobseekercopilot.userprofileservice.model.evidence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EvidenceSupersedeRequest {

    @NotBlank
    @Size(max = 36)
    private String replacementEntryId;
}
