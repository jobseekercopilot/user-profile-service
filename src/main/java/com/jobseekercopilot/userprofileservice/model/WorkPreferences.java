package com.jobseekercopilot.userprofileservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkPreferences {

    @Valid
    @Embedded
    private PostcodeLocation location;

    @Min(0)
    @Max(500)
    private Integer commuteRange;

    @ElementCollection
    @CollectionTable(
            name = "user_profile_commute_travel_modes",
            joinColumns = @JoinColumn(name = "user_profile_id"),
            foreignKey = @ForeignKey(name = "fk_profile_commute_modes_profile"))
    @Enumerated(EnumType.STRING)
    @Column(name = "travel_mode", nullable = false, length = 16)
    @Size(max = 2)
    private Set<CommuteTravelMode> commuteTravelModes = new LinkedHashSet<>();

    @Min(5)
    @Max(180)
    @Column(name = "maximum_driving_minutes")
    private Integer maximumDrivingMinutes;

    @Min(5)
    @Max(180)
    @Column(name = "maximum_transit_minutes")
    private Integer maximumTransitMinutes;

    @ElementCollection
    @CollectionTable(
            name = "user_profile_employment_types",
            joinColumns = @JoinColumn(name = "user_profile_id"),
            foreignKey = @ForeignKey(name = "fk_profile_employment_types_profile"))
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 24)
    @Size(max = 5)
    private Set<EmploymentType> employmentTypes = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "user_profile_working_patterns",
            joinColumns = @JoinColumn(name = "user_profile_id"),
            foreignKey = @ForeignKey(name = "fk_profile_working_patterns_profile"))
    @Enumerated(EnumType.STRING)
    @Column(name = "working_pattern", nullable = false, length = 24)
    @Size(max = 8)
    private Set<WorkingPattern> workingPatterns = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "user_profile_workplace_arrangements",
            joinColumns = @JoinColumn(name = "user_profile_id"),
            foreignKey = @ForeignKey(name = "fk_profile_workplace_arrangements_profile"))
    @Enumerated(EnumType.STRING)
    @Column(name = "workplace_arrangement", nullable = false, length = 16)
    @Size(max = 3)
    private Set<WorkplaceArrangement> workplaceArrangements = new LinkedHashSet<>();

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Min(0)
    @Max(3650)
    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @AssertTrue
    @JsonIgnore
    public boolean isAvailabilityConsistent() {
        return availableFrom == null || noticePeriodDays == null;
    }

    @AssertTrue
    @JsonIgnore
    public boolean isCommuteModeConfigurationConsistent() {
        Set<CommuteTravelMode> modes = commuteTravelModes == null ? Set.of() : commuteTravelModes;
        return (maximumDrivingMinutes == null || modes.contains(CommuteTravelMode.DRIVE))
                && (maximumTransitMinutes == null || modes.contains(CommuteTravelMode.TRANSIT));
    }
}
