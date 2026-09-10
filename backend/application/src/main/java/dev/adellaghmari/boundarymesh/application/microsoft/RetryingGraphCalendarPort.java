package dev.adellaghmari.boundarymesh.application.microsoft;

import java.time.Duration;

public final class RetryingGraphCalendarPort implements GraphCalendarPort {

    private final GraphCalendarPort delegate;

    public RetryingGraphCalendarPort(GraphCalendarPort delegate) {
        this.delegate = delegate;
    }

    @Override
    public java.util.List<ProviderCalendarEvent> readUpcoming(int limit) {
        GraphProviderException last = null;
        for (int attempt = 1; attempt <= GraphReadRetry.MAX_ATTEMPTS; attempt++) {
            try {
                return delegate.readUpcoming(limit);
            } catch (GraphProviderException ex) {
                last = ex;
                if (!ex.retryable() || attempt == GraphReadRetry.MAX_ATTEMPTS) {
                    throw ex;
                }
                sleep(GraphReadRetry.delay(ex));
            }
        }
        throw last;
    }

    private static void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GraphProviderException(0, "graph read interrupted", null);
        }
    }
}
