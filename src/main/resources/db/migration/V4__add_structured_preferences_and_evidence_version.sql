ALTER TABLE user_profile
    ADD COLUMN available_from DATE;

ALTER TABLE user_profile
    ADD COLUMN notice_period_days INTEGER;

ALTER TABLE user_profile
    ADD CONSTRAINT ck_user_profile_notice_period
        CHECK (notice_period_days IS NULL OR notice_period_days BETWEEN 0 AND 3650);

ALTER TABLE user_profile
    ADD CONSTRAINT ck_user_profile_availability_choice
        CHECK (available_from IS NULL OR notice_period_days IS NULL);

CREATE TABLE user_profile_employment_types (
    user_profile_id BIGINT NOT NULL,
    employment_type VARCHAR(24) NOT NULL,
    CONSTRAINT pk_user_profile_employment_types PRIMARY KEY (user_profile_id, employment_type),
    CONSTRAINT fk_profile_employment_types_profile
        FOREIGN KEY (user_profile_id) REFERENCES user_profile (id) ON DELETE CASCADE,
    CONSTRAINT ck_profile_employment_type
        CHECK (employment_type IN ('PERMANENT', 'FIXED_TERM', 'TEMPORARY', 'APPRENTICESHIP', 'CONTRACT'))
);

CREATE TABLE user_profile_working_patterns (
    user_profile_id BIGINT NOT NULL,
    working_pattern VARCHAR(24) NOT NULL,
    CONSTRAINT pk_user_profile_working_patterns PRIMARY KEY (user_profile_id, working_pattern),
    CONSTRAINT fk_profile_working_patterns_profile
        FOREIGN KEY (user_profile_id) REFERENCES user_profile (id) ON DELETE CASCADE,
    CONSTRAINT ck_profile_working_pattern
        CHECK (working_pattern IN ('FULL_TIME', 'PART_TIME', 'FLEXIBLE', 'DAY', 'EVENING', 'NIGHT', 'WEEKEND', 'SHIFT'))
);

CREATE TABLE user_profile_workplace_arrangements (
    user_profile_id BIGINT NOT NULL,
    workplace_arrangement VARCHAR(16) NOT NULL,
    CONSTRAINT pk_user_profile_workplace_arrangements PRIMARY KEY (user_profile_id, workplace_arrangement),
    CONSTRAINT fk_profile_workplace_arrangements_profile
        FOREIGN KEY (user_profile_id) REFERENCES user_profile (id) ON DELETE CASCADE,
    CONSTRAINT ck_profile_workplace_arrangement
        CHECK (workplace_arrangement IN ('ONSITE', 'HYBRID', 'REMOTE'))
);

ALTER TABLE evidence_entry
    ADD COLUMN entry_version BIGINT NOT NULL DEFAULT 0;
