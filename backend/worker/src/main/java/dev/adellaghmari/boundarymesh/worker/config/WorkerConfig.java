package dev.adellaghmari.boundarymesh.worker.config;

import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.InMemoryDeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.worker.connector.ConnectorEndpointProvider;
import dev.adellaghmari.boundarymesh.worker.connector.ProviderException;
import dev.adellaghmari.boundarymesh.worker.connector.ResilientConnectorClient;
import dev.adellaghmari.boundarymesh.worker.stub.SyntheticProviderServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
@EnableConfigurationProperties(WorkerProperties.class)
public class WorkerConfig {

    @Bean
    @Profile({"local", "test"})
    DeliveryIdempotencyStore deliveryIdempotencyStore() {
        return new InMemoryDeliveryIdempotencyStore();
    }

    @Bean
    @Profile({"local", "test"})
    ConnectorEndpointProvider syntheticConnectorEndpoints(SyntheticProviderServer providers) {
        return destination -> providers.baseUrl();
    }

    @Bean
    @Profile("prod")
    ConnectorEndpointProvider productionConnectorEndpoints(WorkerProperties properties) {
        return properties::providerBaseUrl;
    }

    @Bean
    RetryRegistry retryRegistry(WorkerProperties properties) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.retryMaxAttempts())
                .waitDuration(properties.retryWait())
                .retryOnException(ex -> ex instanceof ProviderException provider && provider.retryable())
                .failAfterMaxAttempts(true)
                .build();
        return RetryRegistry.of(config);
    }

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(WorkerProperties properties) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(properties.circuitWindow())
                .minimumNumberOfCalls(properties.circuitWindow())
                .failureRateThreshold(50f)
                .waitDurationInOpenState(properties.circuitOpenWait())
                .permittedNumberOfCallsInHalfOpenState(1)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordException(ex -> ex instanceof ProviderException provider && provider.retryable())
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        for (String destination : Set.of("COVECRM", "LUMENWAY", "RIVERTIDE")) {
            registry.circuitBreaker(destination);
        }
        return registry;
    }

    @Bean
    TimeLimiterRegistry timeLimiterRegistry(WorkerProperties properties) {
        return TimeLimiterRegistry.of(TimeLimiterConfig.custom()
                .timeoutDuration(properties.timeout())
                .cancelRunningFuture(true)
                .build());
    }

    @Bean
    RestClientCustomizer restClientTimeout(WorkerProperties properties) {
        return builder -> {
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
            factory.setReadTimeout(properties.timeout());
            builder.requestFactory(factory);
        };
    }

    @Bean
    ResilientConnectorClient resilientConnectorClient(
            org.springframework.web.client.RestClient.Builder builder,
            CircuitBreakerRegistry circuitBreakers,
            RetryRegistry retries,
            TimeLimiterRegistry timeLimiters,
            WorkerProperties properties,
            MeterRegistry meters) {
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakers).bindTo(meters);
        TaggedRetryMetrics.ofRetryRegistry(retries).bindTo(meters);
        return new ResilientConnectorClient(
                builder, circuitBreakers, retries, timeLimiters, properties.retryAfterCap());
    }
}
