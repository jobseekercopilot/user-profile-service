package com.jobseekercopilot.userprofileservice.model;

import com.jobseekercopilot.userprofileservice.validation.HttpsUrl;
import com.jobseekercopilot.userprofileservice.validation.ProfileConstraints;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A user-declared labelled professional HTTPS link")
public class ProfessionalLink {

    @NotBlank
    @Size(max = ProfileConstraints.MAX_PROFESSIONAL_LINK_LABEL_LENGTH)
    @Pattern(regexp = ProfileConstraints.PROFESSIONAL_LINK_LABEL_PATTERN)
    @Column(name = "label", nullable = false, length = ProfileConstraints.MAX_PROFESSIONAL_LINK_LABEL_LENGTH)
    @Schema(example = "GitHub", maxLength = ProfileConstraints.MAX_PROFESSIONAL_LINK_LABEL_LENGTH)
    private String label;

    @NotBlank
    @Size(max = ProfileConstraints.MAX_PROFESSIONAL_LINK_URL_LENGTH)
    @HttpsUrl
    @Column(name = "link_url", nullable = false, length = ProfileConstraints.MAX_PROFESSIONAL_LINK_URL_LENGTH)
    @Schema(
            example = "https://github.com/example-developer",
            pattern = "^https://",
            maxLength = ProfileConstraints.MAX_PROFESSIONAL_LINK_URL_LENGTH)
    private String url;
}
