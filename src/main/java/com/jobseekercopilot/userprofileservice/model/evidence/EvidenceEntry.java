package com.jobseekercopilot.userprofileservice.model.evidence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobseekercopilot.userprofileservice.model.UserProfile;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evidence_entry", uniqueConstraints = {
        @UniqueConstraint(name = "uk_evidence_entry_migration",
                columnNames = {"user_profile_id", "migration_key"})
})
@Getter
@Setter
@NoArgsConstructor
public class EvidenceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(name = "entry_id", nullable = false, unique = true, length = 36)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String entryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_evidence_entry_profile"))
    @JsonIgnore
    private UserProfile userProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EvidenceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EvidenceVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EvidenceLifecycle lifecycle;

    @Column(name = "superseded_by_entry_id", length = 36)
    private String supersededByEntryId;

    @Column(name = "migration_key", length = 160)
    @JsonIgnore
    private String migrationKey;

    @Column(name = "source_hash", length = 64)
    @JsonIgnore
    private String sourceHash;

    @Column(name = "review_required", nullable = false)
    private boolean reviewRequired;

    @OneToMany(mappedBy = "evidenceEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("revisionNumber ASC")
    private List<EvidenceRevision> revisions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant updatedAt;

    public void addRevision(EvidenceRevision revision) {
        revision.setEvidenceEntry(this);
        revisions.add(revision);
    }

    @PrePersist
    void assignMetadata() {
        if (entryId == null) {
            entryId = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
