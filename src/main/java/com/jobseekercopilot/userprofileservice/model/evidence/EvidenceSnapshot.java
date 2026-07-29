package com.jobseekercopilot.userprofileservice.model.evidence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobseekercopilot.userprofileservice.model.UserProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evidence_snapshot")
@Getter
@Setter
@NoArgsConstructor
public class EvidenceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(name = "snapshot_id", nullable = false, unique = true, length = 36)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String snapshotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_evidence_snapshot_profile"))
    @JsonIgnore
    private UserProfile userProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EvidenceSnapshotPurpose purpose;

    @Column(name = "profile_revision_id", nullable = false, length = 36)
    private String profileRevisionId;

    @Column(name = "profile_content_digest", nullable = false, length = 64)
    private String profileContentDigest;

    @ElementCollection
    @CollectionTable(name = "evidence_snapshot_section",
            joinColumns = @JoinColumn(name = "evidence_snapshot_id"),
            foreignKey = @ForeignKey(name = "fk_evidence_snapshot_section_snapshot"))
    @OrderColumn(name = "position")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private List<EvidenceCategory> sectionOrder = new ArrayList<>();

    @OneToMany(mappedBy = "evidenceSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<EvidenceSnapshotSelection> selections = new ArrayList<>();

    @Column(name = "snapshot_digest", nullable = false, length = 64)
    private String snapshotDigest;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant createdAt;

    public void addSelection(EvidenceSnapshotSelection selection) {
        selection.setEvidenceSnapshot(this);
        selection.setPosition(selections.size());
        selections.add(selection);
    }

    @PrePersist
    void assignImmutableMetadata() {
        if (snapshotId == null) {
            snapshotId = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
