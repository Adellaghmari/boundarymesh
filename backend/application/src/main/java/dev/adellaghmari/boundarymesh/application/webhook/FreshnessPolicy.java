package dev.adellaghmari.boundarymesh.application.webhook;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Accepted signed-timestamp window. Default ±300 seconds. */
public record FreshnessPolicy(Duration window) {

    public static final Duration DEFAULT_WINDOW = Duration.ofSeconds(300);

    public FreshnessPolicy {
        Objects.requireNonNull(window, "window");
        if (window.isNegative() || window.isZero() || window.compareTo(Duration.ofSeconds(600)) > 0) {
            throw new IllegalArgumentException("freshness window must be 1–600 seconds");
        }
    }

    public static FreshnessPolicy ofSeconds(long seconds) {
        return new FreshnessPolicy(Duration.ofSeconds(seconds));
    }

    public Instant lowerBound(Instant now) {
        return now.minus(window);
    }

    public Instant upperBound(Instant now) {
        return now.plus(window);
    }
}
