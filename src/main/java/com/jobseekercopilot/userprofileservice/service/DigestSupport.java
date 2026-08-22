package com.jobseekercopilot.userprofileservice.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class DigestSupport {

    private DigestSupport() {
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    static void append(StringBuilder canonical, Object value) {
        if (value == null) {
            canonical.append("-1:");
            return;
        }
        String text = value.toString();
        canonical.append(text.length()).append(':').append(text);
    }
}
