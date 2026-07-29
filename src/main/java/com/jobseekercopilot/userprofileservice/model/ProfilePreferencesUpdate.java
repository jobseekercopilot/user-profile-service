package com.jobseekercopilot.userprofileservice.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfilePreferencesUpdate {

    @Size(max = 100)
    private List<@NotBlank @Size(max = 100) String> skills = new ArrayList<>();

    @Valid
    private Aspirations aspirations;

    @Valid
    private WorkPreferences workPreferences;
}
