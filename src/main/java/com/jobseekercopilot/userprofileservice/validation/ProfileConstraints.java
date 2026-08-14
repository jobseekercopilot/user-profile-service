package com.jobseekercopilot.userprofileservice.validation;

public final class ProfileConstraints {

    public static final int MAX_SKILLS = 100;
    public static final int MAX_QUALIFICATIONS = 50;
    public static final int MAX_ROLES = 50;
    public static final int MAX_TARGET_ROLES = 50;
    public static final int MAX_PROFESSIONAL_LINKS = 8;
    public static final int MAX_PROFESSIONAL_PHONE_LENGTH = 40;
    public static final int MAX_PROFESSIONAL_LINK_LABEL_LENGTH = 40;
    public static final int MAX_PROFESSIONAL_LINK_URL_LENGTH = 512;

    public static final String PROFESSIONAL_PHONE_PATTERN =
            "^\\s*$|^(?=(?:\\D*\\d){7,15}\\D*$)[+0-9() .-]+$";
    public static final String PROFESSIONAL_LINK_LABEL_PATTERN = "^[^\\p{Cc}]+$";

    public static final String POSTCODE_PATTERN =
            "(?i)^\\s*(?:(?:GIR\\s?0AA|[A-PR-UWYZ][0-9][0-9A-HJKSTUW]?\\s?[0-9][ABD-HJLNP-UW-Z]{2}|"
                    + "[A-PR-UWYZ][A-HK-Y][0-9][0-9ABEHMNPRV-Y]?\\s?[0-9][ABD-HJLNP-UW-Z]{2}|"
                    + "[A-PR-UWYZ][0-9][A-HJKPSTUW]?|[A-PR-UWYZ][A-HK-Y][0-9][ABEHMNPRV-Y]?))?\\s*$";

    private ProfileConstraints() {
    }
}
