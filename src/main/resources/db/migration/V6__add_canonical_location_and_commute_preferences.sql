ALTER TABLE user_profile ADD COLUMN location_id VARCHAR(36);
ALTER TABLE user_profile ADD COLUMN location_display_name VARCHAR(200);
ALTER TABLE user_profile ADD COLUMN location_country_code VARCHAR(2);
ALTER TABLE user_profile ADD COLUMN location_type VARCHAR(24);
ALTER TABLE user_profile ADD COLUMN location_precision VARCHAR(32);
ALTER TABLE user_profile ADD COLUMN location_confidence VARCHAR(16);
ALTER TABLE user_profile ADD COLUMN google_place_id VARCHAR(255);
ALTER TABLE user_profile ADD COLUMN postcodes_io_place_id VARCHAR(128);
ALTER TABLE user_profile ADD COLUMN display_name_source VARCHAR(24);
ALTER TABLE user_profile ADD COLUMN postcode_source VARCHAR(24);
ALTER TABLE user_profile ADD COLUMN coordinates_source VARCHAR(24);
ALTER TABLE user_profile ADD COLUMN maximum_driving_minutes INTEGER;
ALTER TABLE user_profile ADD COLUMN maximum_transit_minutes INTEGER;

ALTER TABLE user_profile ADD CONSTRAINT ck_profile_maximum_driving_minutes
    CHECK (maximum_driving_minutes IS NULL OR maximum_driving_minutes BETWEEN 5 AND 180);
ALTER TABLE user_profile ADD CONSTRAINT ck_profile_maximum_transit_minutes
    CHECK (maximum_transit_minutes IS NULL OR maximum_transit_minutes BETWEEN 5 AND 180);

CREATE TABLE user_profile_commute_travel_modes (
    user_profile_id BIGINT NOT NULL,
    travel_mode VARCHAR(16) NOT NULL,
    CONSTRAINT pk_user_profile_commute_modes PRIMARY KEY (user_profile_id, travel_mode),
    CONSTRAINT fk_profile_commute_modes_profile
        FOREIGN KEY (user_profile_id) REFERENCES user_profile (id) ON DELETE CASCADE,
    CONSTRAINT ck_profile_commute_mode CHECK (travel_mode IN ('DRIVE', 'TRANSIT'))
);

UPDATE user_profile
SET location_country_code = 'GB',
    location_type = 'POSTCODE',
    location_precision = 'POSTCODE_CENTROID',
    location_confidence = 'VERIFIED',
    display_name_source = 'LEGACY',
    postcode_source = 'LEGACY',
    coordinates_source = CASE
        WHEN latitude IS NOT NULL AND longitude IS NOT NULL THEN 'LEGACY'
        ELSE NULL
    END
WHERE postcode IS NOT NULL;
