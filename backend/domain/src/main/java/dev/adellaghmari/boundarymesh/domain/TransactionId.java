package dev.adellaghmari.boundarymesh.domain;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stable identifier for one boundary-crossing attempt.
 */
public record TransactionId(String value) {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public TransactionId {
        Objects.requireNonNull(value, "transaction id");
        String trimmed = value.trim();
        if (!UUID_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("TRANSACTION_ID_INVALID", "transaction id must be a UUID");
        }
        value = trimmed.toLowerCase();
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    public static TransactionId random() {
        return new TransactionId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
