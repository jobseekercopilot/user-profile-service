package com.jobseekercopilot.userprofileservice.model.evidence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evidence_fact", uniqueConstraints = {
        @UniqueConstraint(name = "uk_evidence_fact_position",
                columnNames = {"evidence_revision_id", "position"})
})
@Getter
@Setter
@NoArgsConstructor
public class EvidenceFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(name = "fact_id", nullable = false, unique = true, length = 36)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String factId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_revision_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_evidence_fact_revision"))
    @JsonIgnore
    private EvidenceRevision evidenceRevision;

    @NotBlank
    @Size(max = 64)
    @Column(name = "fact_type", nullable = false, length = 64)
    private String factType;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "fact_value", nullable = false, length = 2000)
    private String factValue;

    @Column(name = "numeric_claim", nullable = false)
    private boolean numericClaim;

    @Column(nullable = false)
    private int position;

    @PrePersist
    void assignPublicId() {
        if (factId == null) {
            factId = UUID.randomUUID().toString();
        }
    }
}
