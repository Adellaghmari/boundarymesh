package dev.adellaghmari.boundarymesh.domain.delivery;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record DeliveryInstructionId(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");

    public DeliveryInstructionId {
        Objects.requireNonNull(value, "delivery instruction id");
        String trimmed = value.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("DELIVERY_ID_INVALID", "delivery instruction id is invalid");
        }
        value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
