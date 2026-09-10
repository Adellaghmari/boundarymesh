package dev.adellaghmari.boundarymesh.application.microsoft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetryingGraphCalendarPortTest {

    @Test
    void retriesRetryable429ThenSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        GraphCalendarPort flaky = limit -> {
            if (attempts.incrementAndGet() < 2) {
                throw new GraphProviderException(429, "slow down", "0");
            }
            return List.of();
        };
        assertThat(new RetryingGraphCalendarPort(flaky).readUpcoming(5)).isEmpty();
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void doesNotRetryForbidden() {
        AtomicInteger attempts = new AtomicInteger();
        GraphCalendarPort forbidden = limit -> {
            attempts.incrementAndGet();
            throw new GraphProviderException(403, "missing permission", null);
        };
        assertThatThrownBy(() -> new RetryingGraphCalendarPort(forbidden).readUpcoming(5))
                .isInstanceOf(GraphProviderException.class)
                .extracting(ex -> ((GraphProviderException) ex).status())
                .isEqualTo(403);
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void delayHonorsRetryAfterSeconds() {
        assertThat(GraphReadRetry.delay(new GraphProviderException(429, "wait", "0")))
                .isEqualTo(Duration.ofMillis(1));
        assertThat(GraphReadRetry.delay(new GraphProviderException(429, "wait", "2")))
                .isEqualTo(Duration.ofSeconds(2));
    }
}
