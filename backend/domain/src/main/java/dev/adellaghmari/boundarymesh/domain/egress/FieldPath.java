package dev.adellaghmari.boundarymesh.domain.egress;

import dev.adellaghmari.boundarymesh.domain.exception.InvalidInputException;
import java.util.Objects;
import java.util.regex.Pattern;

/** RFC 6901 JSON Pointer. */
public record FieldPath(String pointer) {

    private static final Pattern PATTERN = Pattern.compile("^(/([^/~]|~[01])*)*$");

    public FieldPath {
        Objects.requireNonNull(pointer, "pointer");
        if (!PATTERN.matcher(pointer).matches()) {
            throw new InvalidInputException("FIELD_PATH_INVALID", "field path must be a JSON Pointer");
        }
    }

    public static FieldPath parse(String raw) {
        return new FieldPath(raw == null ? "" : raw.trim());
    }

    @Override
    public String toString() {
        return pointer;
    }
}
