package com.jobseekercopilot.userprofileservice.model.evidence;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EvidenceSnapshotRequest {

    @NotNull
    private EvidenceSnapshotPurpose purpose;

    @NotEmpty
    @Size(max = 50)
    private List<@NotNull @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-"
                    + "[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$") String> entryIds =
            new ArrayList<>();

    @NotEmpty
    @Size(max = 9)
    private List<@NotNull EvidenceCategory> sectionOrder = new ArrayList<>();
}
