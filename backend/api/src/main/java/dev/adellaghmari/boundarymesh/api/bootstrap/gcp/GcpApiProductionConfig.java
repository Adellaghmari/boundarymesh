package dev.adellaghmari.boundarymesh.api.bootstrap.gcp;

import dev.adellaghmari.boundarymesh.application.attestation.AttestationRepository;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationSigner;
import dev.adellaghmari.boundarymesh.application.attestation.AttestationVerifier;
import dev.adellaghmari.boundarymesh.application.audit.AuditRepository;
import dev.adellaghmari.boundarymesh.application.connector.ConnectorSafetyPort;
import dev.adellaghmari.boundarymesh.application.connector.LocalConnectorSafetyPort;
import dev.adellaghmari.boundarymesh.application.credential.InMemoryCredentialCatalog;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.application.graph.ObservedCrossingStore;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxDeliveryPort;
import dev.adellaghmari.boundarymesh.application.outbox.OutboxRepository;
import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineRepository;
import dev.adellaghmari.boundarymesh.application.webhook.IdempotencyStore;
import dev.adellaghmari.boundarymesh.application.webhook.ReplayNonceStore;
import dev.adellaghmari.boundarymesh.application.webhook.WebhookSecretProvider;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationSignature;
import dev.adellaghmari.boundarymesh.domain.attestation.SignerKeyReference;
import dev.adellaghmari.boundarymesh.gcp.runtime.GcpApiRuntime;
import dev.adellaghmari.boundarymesh.gcp.runtime.GcpProductionCoordinates;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Production-only composition boundary. Google SDK types remain inside {@code backend/gcp}. */
@Configuration
@Profile("prod")
public class GcpApiProductionConfig {

    @Bean(destroyMethod = "close")
    GcpApiRuntime gcpApiRuntime(
            @Value("${boundarymesh.gcp.project}") String project,
            @Value("${boundarymesh.gcp.location}") String location,
            @Value("${boundarymesh.gcp.firestore-database}") String firestoreDatabase,
            @Value("${boundarymesh.gcp.pubsub-topic}") String pubSubTopic,
            @Value("${boundarymesh.gcp.pubsub-subscription}") String pubSubSubscription,
            @Value("${boundarymesh.gcp.kms-key-ring}") String kmsKeyRing,
            @Value("${boundarymesh.gcp.kms-crypto-key}") String kmsCryptoKey,
            @Value("${boundarymesh.gcp.kms-key-version}") String kmsKeyVersion) {
        return GcpApiRuntime.connect(new GcpProductionCoordinates(
                project,
                location,
                firestoreDatabase,
                pubSubTopic,
                pubSubSubscription,
                kmsKeyRing,
                kmsCryptoKey,
                kmsKeyVersion));
    }

    @Bean
    InMemoryCredentialCatalog credentialCatalog(Clock clock) {
        return InMemoryCredentialCatalog.productionSyntheticDefaults(clock.instant());
    }

    @Bean
    AuditRepository gcpAuditRepository(GcpApiRuntime runtime) {
        return runtime.auditRepository();
    }

    @Bean
    DeliveryIdempotencyStore gcpDeliveryIdempotencyStore(GcpApiRuntime runtime) {
        return runtime.deliveryIdempotencyStore();
    }

    @Bean
    AttestationRepository gcpAttestationRepository(GcpApiRuntime runtime) {
        return runtime.attestationRepository();
    }

    @Bean(destroyMethod = "")
    AttestationSigner gcpAttestationSigner(GcpApiRuntime runtime) {
        AttestationSigner delegate = runtime.attestationSigner();
        return new AttestationSigner() {
            @Override
            public SignerKeyReference key() {
                return delegate.key();
            }

            @Override
            public AttestationSignature sign(byte[] canonicalBytes) {
                return delegate.sign(canonicalBytes);
            }
        };
    }

    @Bean(destroyMethod = "")
    AttestationVerifier gcpAttestationVerifier(GcpApiRuntime runtime) {
        AttestationVerifier delegate = runtime.attestationVerifier();
        return delegate::verify;
    }

    @Bean
    QuarantineRepository gcpQuarantineRepository(GcpApiRuntime runtime) {
        return runtime.quarantineRepository();
    }

    @Bean
    ReplayNonceStore gcpReplayNonceStore(GcpApiRuntime runtime) {
        return runtime.replayNonceStore();
    }

    @Bean
    IdempotencyStore gcpIdempotencyStore(GcpApiRuntime runtime) {
        return runtime.idempotencyStore();
    }

    @Bean
    ObservedCrossingStore gcpObservedCrossingStore(GcpApiRuntime runtime) {
        return runtime.observedCrossingStore();
    }

    @Bean
    OutboxRepository gcpOutboxRepository(GcpApiRuntime runtime) {
        return runtime.outboxRepository();
    }

    @Bean
    WebhookSecretProvider gcpWebhookSecretProvider(GcpApiRuntime runtime) {
        return runtime.webhookSecretProvider();
    }

    @Bean
    DeliveryPort gcpOutboxDeliveryPort(GcpApiRuntime runtime, OutboxRepository outbox, Clock clock) {
        return new OutboxDeliveryPort(outbox, runtime.messagePublisher(), clock);
    }

    @Bean
    ConnectorSafetyPort productionConnectorCatalog() {
        // Dynamic worker circuit telemetry has no cloud-neutral transport yet. The current catalog
        // still enforces the known destination allow-list; provider delivery remains worker-only.
        return new LocalConnectorSafetyPort();
    }
}
