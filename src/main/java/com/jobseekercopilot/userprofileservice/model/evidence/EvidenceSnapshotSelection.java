package com.jobseekercopilot.userprofileservice.model.evidence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evidence_snapshot_selection", uniqueConstraints = {
        @UniqueConstraint(name = "uk_snapshot_selection_position",
                columnNames = {"evidence_snapshot_id", "position"}),
        @UniqueConstraint(name = "uk_snapshot_selection_entry",
                columnNames = {"evidence_snapshot_id", "entry_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class EvidenceSnapshotSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_snapshot_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_snapshot_selection_snapshot"))
    @JsonIgnore
    private EvidenceSnapshot evidenceSnapshot;

    @Column(name = "entry_id", nullable = false, length = 36)
    private String entryId;

    @Column(name = "revision_id", nullable = false, length = 36)
    private String revisionId;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EvidenceCategory category;

    @Column(name = "content_digest", nullable = false, length = 64)
    private String contentDigest;

    @Column(nullable = false)
    @JsonIgnore
    private int position;

    @OneToMany(mappedBy = "selection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<EvidenceSnapshotFact> facts = new ArrayList<>();

    public void addFact(EvidenceSnapshotFact fact) {
        fact.setSelection(this);
        fact.setPosition(facts.size());
        facts.add(fact);
    }
}
