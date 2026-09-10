package dev.adellaghmari.boundarymesh.application.microsoft;

import java.time.Duration;

/**
 * Bounded Graph retry. Delivery-path Resilience4j stays on the Camel worker. Graph reads honor
 * Retry-After when present and never hammer the provider.
 */
public final class GraphReadRetry {

    static final int MAX_ATTEMPTS = 3;

    private GraphReadRetry() {}

    public static Duration delay(GraphProviderException ex) {
        if (ex.retryAfter() != null) {
            try {
                long seconds = Long.parseLong(ex.retryAfter().trim());
                if (seconds <= 0) {
                    return Duration.ofMillis(1);
                }
                return Duration.ofSeconds(Math.min(seconds, 8));
            } catch (NumberFormatException ignored) {
                return Duration.ofSeconds(1);
            }
        }
        return Duration.ofMillis(200);
    }
}
