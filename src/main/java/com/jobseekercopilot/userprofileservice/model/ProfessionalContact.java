package com.jobseekercopilot.userprofileservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobseekercopilot.userprofileservice.validation.ProfileConstraints;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "profile_professional_contact")
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Private, user-declared contact details available to owner-authorised document workflows")
public class ProfessionalContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @JsonIgnore
    @OneToOne(optional = false)
    @JoinColumn(
            name = "user_profile_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_professional_contact_profile"))
    private UserProfile userProfile;

    @Size(max = ProfileConstraints.MAX_PROFESSIONAL_PHONE_LENGTH)
    @Pattern(regexp = ProfileConstraints.PROFESSIONAL_PHONE_PATTERN)
    @Column(name = "phone", length = ProfileConstraints.MAX_PROFESSIONAL_PHONE_LENGTH)
    @Schema(
            description = "Optional user-declared professional telephone number; no value is inferred from CV content",
            example = "+44 20 7946 0958",
            maxLength = ProfileConstraints.MAX_PROFESSIONAL_PHONE_LENGTH,
            pattern = ProfileConstraints.PROFESSIONAL_PHONE_PATTERN)
    private String phone;

    @Size(max = ProfileConstraints.MAX_PROFESSIONAL_LINKS)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "profile_professional_link",
            joinColumns = @JoinColumn(name = "professional_contact_id"),
            foreignKey = @ForeignKey(name = "fk_professional_link_contact"))
    @OrderColumn(name = "position")
    private List<@NotNull @Valid ProfessionalLink> links = new ArrayList<>();
}
