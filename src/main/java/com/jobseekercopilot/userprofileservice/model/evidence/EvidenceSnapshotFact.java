package com.jobseekercopilot.userprofileservice.model.evidence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evidence_snapshot_fact", uniqueConstraints = {
        @UniqueConstraint(name = "uk_snapshot_fact_position",
                columnNames = {"snapshot_selection_id", "position"}),
        @UniqueConstraint(name = "uk_snapshot_fact_id",
                columnNames = {"snapshot_selection_id", "fact_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class EvidenceSnapshotFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_selection_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_snapshot_fact_selection"))
    @JsonIgnore
    private EvidenceSnapshotSelection selection;

    @Column(name = "fact_id", nullable = false, length = 36)
    private String factId;

    @Column(name = "fact_type", nullable = false, length = 64)
    private String factType;

    @Column(name = "fact_value", nullable = false, length = 2000)
    private String factValue;

    @Column(name = "numeric_claim", nullable = false)
    private boolean numericClaim;

    @Column(nullable = false)
    @JsonIgnore
    private int position;
}
