package dev.adellaghmari.boundarymesh.worker.connector;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class RetryAfterParser {

    private RetryAfterParser() {}

    public static Duration parse(String raw, Duration cap) {
        if (raw == null || raw.isBlank()) {
            return Duration.ZERO;
        }
        String value = raw.trim();
        try {
            long seconds = Long.parseLong(value);
            if (seconds < 0) {
                return Duration.ZERO;
            }
            Duration parsed = Duration.ofSeconds(seconds);
            return parsed.compareTo(cap) > 0 ? cap : parsed;
        } catch (NumberFormatException ignored) {
            try {
                Instant when = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(value));
                Duration parsed = Duration.between(Instant.now(), when);
                if (parsed.isNegative()) {
                    return Duration.ZERO;
                }
                return parsed.compareTo(cap) > 0 ? cap : parsed;
            } catch (RuntimeException ex) {
                return Duration.ZERO;
            }
        }
    }
}
