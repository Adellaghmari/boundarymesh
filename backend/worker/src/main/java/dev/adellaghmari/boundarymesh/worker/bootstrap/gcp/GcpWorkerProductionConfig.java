package dev.adellaghmari.boundarymesh.worker.bootstrap.gcp;

import dev.adellaghmari.boundarymesh.application.credential.SecretValueProvider;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryIdempotencyStore;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import dev.adellaghmari.boundarymesh.gcp.pubsub.ContinuousPubSubWorker;
import dev.adellaghmari.boundarymesh.gcp.runtime.GcpProductionCoordinates;
import dev.adellaghmari.boundarymesh.gcp.runtime.GcpWorkerRuntime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Production Worker Pool composition. No HTTP endpoint and no boundary decision engine. */
@Configuration
@Profile("prod")
public class GcpWorkerProductionConfig {

    @Bean(destroyMethod = "close")
    GcpWorkerRuntime gcpWorkerRuntime(
            @Value("${boundarymesh.gcp.project}") String project,
            @Value("${boundarymesh.gcp.location}") String location,
            @Value("${boundarymesh.gcp.firestore-database}") String firestoreDatabase,
            @Value("${boundarymesh.gcp.pubsub-topic}") String pubSubTopic,
            @Value("${boundarymesh.gcp.pubsub-subscription}") String pubSubSubscription,
            @Value("${boundarymesh.gcp.kms-key-ring}") String kmsKeyRing,
            @Value("${boundarymesh.gcp.kms-crypto-key}") String kmsCryptoKey,
            @Value("${boundarymesh.gcp.kms-key-version}") String kmsKeyVersion) {
        return GcpWorkerRuntime.connect(new GcpProductionCoordinates(
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
    DeliveryIdempotencyStore gcpDeliveryIdempotencyStore(GcpWorkerRuntime runtime) {
        return runtime.idempotencyStore();
    }

    @Bean
    SecretValueProvider gcpSecretValueProvider(GcpWorkerRuntime runtime) {
        return runtime.secretValueProvider();
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    ContinuousPubSubWorker continuousPubSubWorker(GcpWorkerRuntime runtime, DeliveryPort deliveryPort) {
        return runtime.newPullWorker(deliveryPort);
    }
}
