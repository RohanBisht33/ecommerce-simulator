package com.ecommerce.util;

public final class RedirectValidator {

    private RedirectValidator() {
    }

    public static String sanitizeRelativePath(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return null;
        }

        String trimmed = redirectUrl.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return null;
        }

        String lower = trimmed.toLowerCase();
        if (lower.startsWith("/http:") || lower.startsWith("/https:") || trimmed.contains("://")) {
            return null;
        }

        return trimmed;
    }
}
