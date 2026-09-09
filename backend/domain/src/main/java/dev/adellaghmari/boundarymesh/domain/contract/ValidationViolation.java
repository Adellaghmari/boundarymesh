package dev.adellaghmari.boundarymesh.domain.contract;

import java.util.Objects;

/**
 * Safe schema evidence. Paths and categories only — never payload values.
 */
public record ValidationViolation(String jsonPointer, String keyword, String expectedCategory, String actualCategory) {

    public ValidationViolation {
        Objects.requireNonNull(jsonPointer, "jsonPointer");
        Objects.requireNonNull(keyword, "keyword");
        expectedCategory = expectedCategory == null ? "" : expectedCategory;
        actualCategory = actualCategory == null ? "" : actualCategory;
        if (looksLikeValue(jsonPointer)
                || looksLikeValue(keyword)
                || looksLikeValue(expectedCategory)
                || looksLikeValue(actualCategory)) {
            throw new IllegalArgumentException("validation evidence must not include payload values");
        }
    }

    private static boolean looksLikeValue(String text) {
        String lower = text.toLowerCase();
        return lower.contains("@") || lower.contains("bearer ") || lower.contains("secret");
    }
}
