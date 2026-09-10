package dev.adellaghmari.boundarymesh.domain.consent;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

public record ConnectorOperation(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9._-]{1,63}$");

    public static final ConnectorOperation ACTIVITY_WRITE = new ConnectorOperation("activity.write");
    public static final ConnectorOperation FULFILLMENT_CREATE = new ConnectorOperation("fulfillment.create");
    public static final ConnectorOperation SETTLEMENT_WRITE = new ConnectorOperation("settlement.write");

    public ConnectorOperation {
        Objects.requireNonNull(value, "operation");
        String trimmed = value.trim();
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidInputException("CONNECTOR_OPERATION_INVALID", "operation must be a lowercase slug");
        }
        value = trimmed;
    }

    @Override
    public String toString() {
        return value;
    }
}
