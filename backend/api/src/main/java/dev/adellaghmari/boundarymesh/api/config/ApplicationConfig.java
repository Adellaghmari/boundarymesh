package dev.adellaghmari.boundarymesh.api.config;

import dev.adellaghmari.boundarymesh.application.BoundaryEnforcementEngine;
import dev.adellaghmari.boundarymesh.application.connector.ConnectorSafetyEvaluator;
import dev.adellaghmari.boundarymesh.application.connector.ConnectorSafetyPort;
import dev.adellaghmari.boundarymesh.application.connector.LocalConnectorSafetyPort;
import dev.adellaghmari.boundarymesh.application.consent.ConsentEvaluator;
import dev.adellaghmari.boundarymesh.application.consent.InMemoryConsentCatalog;
import dev.adellaghmari.boundarymesh.application.contract.ContractEvaluator;
import dev.adellaghmari.boundarymesh.application.contract.ContractRegistry;
import dev.adellaghmari.boundarymesh.application.contract.PayloadSchemaValidator;
import dev.adellaghmari.boundarymesh.application.credential.CredentialEvaluator;
import dev.adellaghmari.boundarymesh.application.credential.InMemoryCredentialCatalog;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingDeliveryService;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryInstructionFactory;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.delivery.RecordingDeliveryPort;
import dev.adellaghmari.boundarymesh.application.egress.EgressEvaluator;
import dev.adellaghmari.boundarymesh.application.egress.EgressPolicyRepository;
import dev.adellaghmari.boundarymesh.application.quarantine.InMemoryQuarantineRepository;
import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineOrchestrator;
import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineRepository;
import dev.adellaghmari.boundarymesh.application.quarantine.ReprocessPayloadStore;
import dev.adellaghmari.boundarymesh.application.webhook.FreshnessPolicy;
import dev.adellaghmari.boundarymesh.application.webhook.IdempotencyStore;
import dev.adellaghmari.boundarymesh.application.webhook.ReplayNonceStore;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookDefenseService;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookSecretProvider;
import dev.adellaghmari.boundarymesh.domain.PolicyVersion;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

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
    ContractEvaluator contractEvaluator(ContractRegistry registry, PayloadSchemaValidator validator) {
        return new ContractEvaluator(registry, validator);
    }

    @Bean
    EgressEvaluator egressEvaluator(EgressPolicyRepository repository) {
        return new EgressEvaluator(repository);
    }

    @Bean
    CrossingComposer crossingComposer(
            BoundaryEnforcementEngine engine,
            ContractEvaluator contracts,
            EgressEvaluator egress,
            CredentialEvaluator credentials,
            ConsentEvaluator consents,
            ConnectorSafetyEvaluator connectors,
            Clock clock) {
        return new CrossingComposer(engine, contracts, egress, credentials, consents, connectors, clock);
    }

    @Bean
    InMemoryCredentialCatalog credentialCatalog(Clock clock) {
        return InMemoryCredentialCatalog.veloraDefaults(clock.instant());
    }

    @Bean
    CredentialEvaluator credentialEvaluator(InMemoryCredentialCatalog catalog) {
        return new CredentialEvaluator(catalog);
    }

    @Bean
    InMemoryConsentCatalog consentCatalog() {
        return InMemoryConsentCatalog.veloraDefaults();
    }

    @Bean
    ConsentEvaluator consentEvaluator(InMemoryConsentCatalog catalog) {
        return new ConsentEvaluator(catalog);
    }

    @Bean
    @Profile("test")
    ConnectorSafetyPort connectorSafetyPort() {
        return new LocalConnectorSafetyPort();
    }

    @Bean
    ConnectorSafetyEvaluator connectorSafetyEvaluator(ConnectorSafetyPort port) {
        return new ConnectorSafetyEvaluator(port);
    }

    @Bean
    DeliveryInstructionFactory deliveryInstructionFactory(Clock clock) {
        return new DeliveryInstructionFactory(clock);
    }

    @Bean
    @Profile("test")
    RecordingDeliveryPort recordingDeliveryPort() {
        return new RecordingDeliveryPort();
    }

    @Bean
    QuarantineRepository quarantineRepository() {
        return new InMemoryQuarantineRepository();
    }

    @Bean
    QuarantineOrchestrator quarantineOrchestrator(QuarantineRepository repository, Clock clock) {
        return new QuarantineOrchestrator(repository, clock);
    }

    @Bean
    ReprocessPayloadStore reprocessPayloadStore() {
        return new ReprocessPayloadStore();
    }

    @Bean
    CrossingDeliveryService crossingDeliveryService(
            CrossingComposer composer,
            DeliveryInstructionFactory instructions,
            DeliveryPort deliveryPort,
            QuarantineOrchestrator quarantine,
            ReprocessPayloadStore payloads) {
        return new CrossingDeliveryService(composer, instructions, deliveryPort, quarantine, payloads);
    }

    @Bean
    WebhookDefenseService webhookDefenseService(
            BoundaryEnforcementEngine engine,
            WebhookSecretProvider secrets,
            ReplayNonceStore replayNonceStore,
            IdempotencyStore idempotencyStore,
            FreshnessPolicy freshnessPolicy,
            Clock clock,
            CrossingComposer composer) {
        return new WebhookDefenseService(
                engine, secrets, replayNonceStore, idempotencyStore, freshnessPolicy, clock, composer);
    }
}
