package dev.adellaghmari.boundarymesh.api.config;

import dev.adellaghmari.boundarymesh.application.BoundaryEnforcementEngine;
import dev.adellaghmari.boundarymesh.application.webhook.FreshnessPolicy;
import dev.adellaghmari.boundarymesh.application.webhook.IdempotencyStore;
import dev.adellaghmari.boundarymesh.application.webhook.ReplayNonceStore;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookDefenseService;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookSecretProvider;
import dev.adellaghmari.boundarymesh.domain.PolicyVersion;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    BoundaryEnforcementEngine boundaryEnforcementEngine(Clock clock, BoundaryMeshProperties properties) {
        return new BoundaryEnforcementEngine(clock, new PolicyVersion(properties.policyVersion()));
    }

    @Bean
    FreshnessPolicy freshnessPolicy(BoundaryMeshProperties properties) {
        return new FreshnessPolicy(properties.webhook().freshnessWindow());
    }

    @Bean
    WebhookDefenseService webhookDefenseService(
            BoundaryEnforcementEngine engine,
            WebhookSecretProvider secrets,
            ReplayNonceStore replayNonceStore,
            IdempotencyStore idempotencyStore,
            FreshnessPolicy freshnessPolicy,
            Clock clock) {
        return new WebhookDefenseService(engine, secrets, replayNonceStore, idempotencyStore, freshnessPolicy, clock);
    }
}
