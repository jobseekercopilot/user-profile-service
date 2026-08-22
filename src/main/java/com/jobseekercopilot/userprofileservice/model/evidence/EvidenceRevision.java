package com.jobseekercopilot.userprofileservice.model.evidence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evidence_revision", uniqueConstraints = {
        @UniqueConstraint(name = "uk_evidence_revision_number",
                columnNames = {"evidence_entry_id", "revision_number"})
})
@Getter
@Setter
@NoArgsConstructor
public class EvidenceRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(name = "revision_id", nullable = false, unique = true, length = 36)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String revisionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_entry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_evidence_revision_entry"))
    @JsonIgnore
    private EvidenceEntry evidenceEntry;

    @Column(name = "revision_number", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_state", nullable = false, length = 24)
    private EvidenceConfirmationState confirmationState;

    @Column(name = "content_digest", nullable = false, length = 64)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String contentDigest;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String heading;

    @Size(max = 200)
    @Column(name = "organisation_context", length = 200)
    private String organisationContext;

    @Size(max = 200)
    @Column(name = "role_title", length = 200)
    private String roleTitle;

    @Size(max = 200)
    @Column(name = "programme_or_subject", length = 200)
    private String programmeOrSubject;

    @Size(max = 200)
    @Column(length = 200)
    private String institution;

    @Size(max = 200)
    @Column(name = "qualification_title", length = 200)
    private String qualificationTitle;

    @Size(max = 200)
    @Column(length = 200)
    private String issuer;

    @Size(max = 100)
    @Column(name = "result_or_status", length = 100)
    private String resultOrStatus;

    @Size(max = 200)
    @Column(name = "project_role", length = 200)
    private String projectRole;

    @Size(max = 4000)
    @Column(length = 4000)
    private String description;

    @Size(max = 4000)
    @Column(length = 4000)
    private String responsibilities;

    @Size(max = 4000)
    @Column(length = 4000)
    private String achievements;

    @Size(max = 1000)
    @Column(name = "career_break_reason", length = 1000)
    private String careerBreakReason;

    @Size(max = 500)
    @Column(name = "private_credential_identifier", length = 500)
    private String privateCredentialIdentifier;

    @Column(nullable = false)
    private boolean ongoing;

    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "precision", column = @Column(name = "start_date_precision", length = 8)),
            @AttributeOverride(name = "year", column = @Column(name = "start_date_year")),
            @AttributeOverride(name = "month", column = @Column(name = "start_date_month")),
            @AttributeOverride(name = "day", column = @Column(name = "start_date_day"))
    })
    private PartialDate startDate;

    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "precision", column = @Column(name = "end_date_precision", length = 8)),
            @AttributeOverride(name = "year", column = @Column(name = "end_date_year")),
            @AttributeOverride(name = "month", column = @Column(name = "end_date_month")),
            @AttributeOverride(name = "day", column = @Column(name = "end_date_day"))
    })
    private PartialDate endDate;

    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "precision", column = @Column(name = "issue_date_precision", length = 8)),
            @AttributeOverride(name = "year", column = @Column(name = "issue_date_year")),
            @AttributeOverride(name = "month", column = @Column(name = "issue_date_month")),
            @AttributeOverride(name = "day", column = @Column(name = "issue_date_day"))
    })
    private PartialDate issueDate;

    @Valid
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "precision", column = @Column(name = "expiry_date_precision", length = 8)),
            @AttributeOverride(name = "year", column = @Column(name = "expiry_date_year")),
            @AttributeOverride(name = "month", column = @Column(name = "expiry_date_month")),
            @AttributeOverride(name = "day", column = @Column(name = "expiry_date_day"))
    })
    private PartialDate expiryDate;

    @ElementCollection
    @CollectionTable(name = "evidence_revision_skill",
            joinColumns = @JoinColumn(name = "evidence_revision_id"),
            foreignKey = @ForeignKey(name = "fk_evidence_skill_revision"))
    @OrderColumn(name = "position")
    @Column(name = "skill", nullable = false, length = 100)
    private List<@NotBlank @Size(max = 100) String> demonstratedSkills = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "evidence_supporting_link",
            joinColumns = @JoinColumn(name = "evidence_revision_id"),
            foreignKey = @ForeignKey(name = "fk_evidence_link_revision"))
    @OrderColumn(name = "position")
    @Column(name = "link_url", nullable = false, length = 2048)
    private List<@NotBlank @Size(max = 2048) String> supportingLinks = new ArrayList<>();

    @OneToMany(mappedBy = "evidenceRevision", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<@Valid EvidenceFact> facts = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by", nullable = false, updatable = false, length = 32)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private EvidenceRevisionCreator createdBy;

    public void addFact(EvidenceFact fact) {
        fact.setEvidenceRevision(this);
        fact.setPosition(facts.size());
        facts.add(fact);
    }

    @PrePersist
    void assignImmutableMetadata() {
        if (revisionId == null) {
            revisionId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
