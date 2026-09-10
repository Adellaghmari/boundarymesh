package dev.adellaghmari.boundarymesh.worker.connector;

import dev.adellaghmari.boundarymesh.application.credential.SecretMaterial;
import dev.adellaghmari.boundarymesh.application.delivery.ProviderScenario;
import dev.adellaghmari.boundarymesh.domain.connector.CircuitState;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public class ResilientConnectorClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientConnectorClient.class);

    private final RestClient restClient;
    private final CircuitBreakerRegistry circuitBreakers;
    private final RetryRegistry retries;
    private final Duration retryAfterCap;

    public ResilientConnectorClient(
            RestClient.Builder builder,
            CircuitBreakerRegistry circuitBreakers,
            RetryRegistry retries,
            io.github.resilience4j.timelimiter.TimeLimiterRegistry ignoredTimeLimiters,
            Duration retryAfterCap) {
        this.restClient = builder.build();
        this.circuitBreakers = circuitBreakers;
        this.retries = retries;
        this.retryAfterCap = retryAfterCap;
    }

    public ConnectorCallResult post(
            String destination, String url, String body, String credentialVersion, SecretMaterial secret) {
        CircuitBreaker breaker = circuitBreakers.circuitBreaker(destination);
        Retry retry = retries.retry(destination);
        AtomicInteger attempts = new AtomicInteger();
        Callable<ConnectorCallResult> decorated =
                CircuitBreaker.decorateCallable(breaker, Retry.decorateCallable(retry, () -> {
                    ConnectorCallResult result =
                            invoke(url, body, credentialVersion, secret, attempts.incrementAndGet());
                    if (result.category() != ProviderFailureCategory.SUCCESS && result.retryable()) {
                        maybeWaitForRetryAfter(result.retryAfter());
                        throw new ProviderException(
                                result.status(), result.category(), true, result.safeMessage(), result.retryAfter());
                    }
                    if (result.category() != ProviderFailureCategory.SUCCESS) {
                        throw new ProviderException(
                                result.status(), result.category(), false, result.safeMessage(), null);
                    }
                    return result;
                }));
        try {
            return decorated.call();
        } catch (CallNotPermittedException ex) {
            return new ConnectorCallResult(
                    0,
                    ProviderFailureCategory.CIRCUIT_OPEN,
                    false,
                    attempts.get(),
                    null,
                    "Circuit is OPEN; provider was not called.",
                    circuitState(breaker));
        } catch (ProviderException ex) {
            return fromProvider(ex, attempts.get(), breaker);
        } catch (Exception ex) {
            ProviderException provider = findProvider(ex);
            if (provider != null) {
                return fromProvider(provider, attempts.get(), breaker);
            }
            log.warn(
                    "Connector call ended with {} after {} attempts",
                    ex.getClass().getSimpleName(),
                    attempts.get());
            return new ConnectorCallResult(
                    0,
                    ProviderFailureCategory.TEMPORARY_FAILURE,
                    true,
                    attempts.get(),
                    null,
                    "Provider call failed after bounded attempts.",
                    circuitState(breaker));
        }
    }

    public CircuitState circuitState(String destination) {
        return circuitState(circuitBreakers.circuitBreaker(destination));
    }

    public void recordFailures(String destination, int count) {
        CircuitBreaker breaker = circuitBreakers.circuitBreaker(destination);
        for (int i = 0; i < count; i++) {
            breaker.onError(
                    0,
                    TimeUnit.MILLISECONDS,
                    new ProviderException(
                            503, ProviderFailureCategory.TEMPORARY_FAILURE, true, "Synthetic circuit probe.", null));
        }
    }

    public void reset(String destination) {
        CircuitBreaker breaker = circuitBreakers.circuitBreaker(destination);
        breaker.reset();
        breaker.transitionToClosedState();
    }

    private static ConnectorCallResult fromProvider(ProviderException ex, int attempts, CircuitBreaker breaker) {
        return new ConnectorCallResult(
                ex.status(),
                ex.category(),
                ex.retryable(),
                attempts,
                ex.retryAfter(),
                ex.getMessage(),
                circuitState(breaker));
    }

    private static ProviderException findProvider(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ProviderException provider) {
                return provider;
            }
            current = current.getCause();
        }
        return null;
    }

    private ConnectorCallResult invoke(
            String url, String body, String credentialVersion, SecretMaterial secret, int attempt) {
        try {
            ResponseEntity<String> response = restClient
                    .post()
                    .uri(URI.create(url))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secret.reveal())
                    .header("X-Boundary-Credential-Version", credentialVersion)
                    .header("X-Boundary-Attempt", String.valueOf(attempt))
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
            log.info(
                    "Connector HTTP completed status={} attempt={}",
                    response.getStatusCode().value(),
                    attempt);
            return new ConnectorCallResult(
                    response.getStatusCode().value(),
                    ProviderFailureCategory.SUCCESS,
                    false,
                    attempt,
                    null,
                    "Provider accepted the delivery.",
                    null);
        } catch (RestClientResponseException ex) {
            String retryAfter = ex.getResponseHeaders() == null
                    ? null
                    : ex.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
            int status = ex.getStatusCode().value();
            return new ConnectorCallResult(
                    status,
                    FailureClassifier.category(status),
                    FailureClassifier.retryable(status),
                    attempt,
                    retryAfter,
                    "Provider returned HTTP " + status + ".",
                    null);
        } catch (ResourceAccessException ex) {
            return new ConnectorCallResult(
                    0, ProviderFailureCategory.TIMEOUT, true, attempt, null, "Provider call timed out.", null);
        }
    }

    private void maybeWaitForRetryAfter(String raw) {
        Duration wait = RetryAfterParser.parse(raw, retryAfterCap);
        if (wait.isZero() || wait.isNegative()) {
            return;
        }
        try {
            Thread.sleep(wait.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    static CircuitState circuitState(CircuitBreaker breaker) {
        return switch (breaker.getState()) {
            case CLOSED -> CircuitState.CLOSED;
            case OPEN, FORCED_OPEN -> CircuitState.OPEN;
            case HALF_OPEN -> CircuitState.HALF_OPEN;
            case DISABLED, METRICS_ONLY -> CircuitState.DISABLED;
        };
    }

    public static String pathFor(ProviderScenario scenario) {
        return switch (scenario) {
            case STATUS_400 -> "/400";
            case STATUS_401 -> "/401";
            case STATUS_403 -> "/403";
            case STATUS_429 -> "/429";
            case STATUS_429_THEN_SUCCESS -> "/429-then-success";
            case STATUS_500 -> "/500";
            case STATUS_503, CIRCUIT_PREP -> "/503";
            case TIMEOUT -> "/timeout";
            default -> "";
        };
    }

    public record ConnectorCallResult(
            int status,
            ProviderFailureCategory category,
            boolean retryable,
            int attempts,
            String retryAfter,
            String safeMessage,
            CircuitState circuitState) {}
}
