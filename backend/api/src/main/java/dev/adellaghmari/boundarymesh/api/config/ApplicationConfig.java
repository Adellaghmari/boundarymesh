package dev.adellaghmari.boundarymesh.api.config;

import dev.adellaghmari.boundarymesh.application.BoundaryEnforcementEngine;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationFactory;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationRepository;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationService;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationSigner;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationVerifier;
import dev.adellaghmari.boundarymesh.application.attestation.InMemoryAttestationRepository;
import dev.adellaghmari.boundarymesh.application.attestation.JcaP256AttestationCrypto;
import dev.adellaghmari.boundarymesh.application.audit.AuditAppender;
import dev.adellaghmari.boundarymesh.application.audit.AuditChainVerifier;
import dev.adellaghmari.boundarymesh.application.audit.AuditRepository;
import dev.adellaghmari.boundarymesh.application.audit.InMemoryAuditRepository;
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
import dev.adellaghmari.boundarymesh.application.crossing.TransactionSubmissionService;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryInstructionFactory;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryStateQueryService;
import dev.adellaghmari.boundarymesh.application.delivery.InMemoryDeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.RecordingDeliveryPort;
import dev.adellaghmari.boundarymesh.application.egress.EgressEvaluator;
import dev.adellaghmari.boundarymesh.application.egress.EgressPolicyRepository;
import dev.adellaghmari.boundarymesh.application.evidence.CrossingEvidenceService;
import dev.adellaghmari.boundarymesh.application.graph.BoundaryGraphAssembler;
import dev.adellaghmari.boundarymesh.application.graph.InMemoryObservedCrossingStore;
import dev.adellaghmari.boundarymesh.application.graph.ObservedCrossingStore;
import dev.adellaghmari.boundarymesh.application.microsoft.CalendarEventNormalizer;
import dev.adellaghmari.boundarymesh.application.microsoft.GraphCalendarPort;
import dev.adellaghmari.boundarymesh.application.microsoft.HarnessGraphCalendarAdapter;
import dev.adellaghmari.boundarymesh.application.microsoft.MicrosoftCalendarCrossingService;
import dev.adellaghmari.boundarymesh.application.microsoft.RetryingGraphCalendarPort;
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
    @Profile("!prod")
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
    @Profile({"local", "test"})
    DeliveryIdempotencyStore localDeliveryIdempotencyStore() {
        return new InMemoryDeliveryIdempotencyStore();
    }

    @Bean
    DeliveryStateQueryService deliveryStateQueryService(DeliveryIdempotencyStore store) {
        return new DeliveryStateQueryService(store);
    }

    @Bean
    @Profile("test")
    RecordingDeliveryPort recordingDeliveryPort() {
        return new RecordingDeliveryPort();
    }

    @Bean
    @Profile({"local", "test"})
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
            ReprocessPayloadStore payloads,
            CrossingEvidenceService evidence) {
        return new CrossingDeliveryService(composer, instructions, deliveryPort, quarantine, payloads, evidence);
    }

    @Bean
    TransactionSubmissionService transactionSubmissionService(CrossingDeliveryService deliveries) {
        return new TransactionSubmissionService(deliveries);
    }

    @Bean
    @Profile({"local", "test"})
    InMemoryAuditRepository auditRepository() {
        return new InMemoryAuditRepository();
    }

    @Bean
    AuditAppender auditAppender(AuditRepository auditRepository, Clock clock) {
        return new AuditAppender(auditRepository, clock);
    }

    @Bean
    AuditChainVerifier auditChainVerifier() {
        return new AuditChainVerifier();
    }

    @Bean
    @Profile({"local", "test"})
    JcaP256AttestationCrypto jcaP256AttestationCrypto() {
        return new JcaP256AttestationCrypto();
    }

    @Bean
    @Profile({"local", "test"})
    InMemoryAttestationRepository attestationRepository() {
        return new InMemoryAttestationRepository();
    }

    @Bean
    AttestationFactory attestationFactory(Clock clock, AttestationSigner signer) {
        return new AttestationFactory(clock, signer);
    }

    @Bean
    AttestationService attestationService(AttestationRepository repository, AttestationVerifier verifier) {
        return new AttestationService(repository, verifier);
    }

    @Bean
    @Profile({"local", "test"})
    ObservedCrossingStore observedCrossingStore() {
        return new InMemoryObservedCrossingStore();
    }

    @Bean
    CrossingEvidenceService crossingEvidenceService(
            AuditAppender audit,
            AuditRepository auditRepository,
            AttestationFactory attestations,
            AttestationRepository attestationRepository,
            ObservedCrossingStore observations) {
        return new CrossingEvidenceService(audit, auditRepository, attestations, attestationRepository, observations);
    }

    @Bean
    BoundaryGraphAssembler boundaryGraphAssembler(ObservedCrossingStore observations, QuarantineRepository quarantine) {
        return new BoundaryGraphAssembler(observations, quarantine);
    }

    @Bean
    HarnessGraphCalendarAdapter harnessGraphCalendarAdapter() {
        return new HarnessGraphCalendarAdapter();
    }

    @Bean
    GraphCalendarPort graphCalendarPort(HarnessGraphCalendarAdapter harness) {
        return new RetryingGraphCalendarPort(harness);
    }

    @Bean
    CalendarEventNormalizer calendarEventNormalizer() {
        return new CalendarEventNormalizer();
    }

    @Bean
    MicrosoftCalendarCrossingService microsoftCalendarCrossingService(
            GraphCalendarPort graphCalendarPort,
            CalendarEventNormalizer calendarEventNormalizer,
            CrossingDeliveryService crossingDeliveryService,
            AuditAppender auditAppender) {
        return new MicrosoftCalendarCrossingService(
                graphCalendarPort, calendarEventNormalizer, crossingDeliveryService, auditAppender);
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
