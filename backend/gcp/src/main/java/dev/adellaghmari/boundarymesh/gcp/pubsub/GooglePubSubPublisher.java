package dev.adellaghmari.boundarymesh.gcp.pubsub;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryInstructionMessage;
import dev.adellaghmari.boundarymesh.application.outbox.MessagePublishException;
import dev.adellaghmari.boundarymesh.application.outbox.MessagePublisherPort;
import dev.adellaghmari.boundarymesh.application.outbox.PublishFailureKind;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Official Java Pub/Sub publisher. Not default-wired into the API. */
public final class GooglePubSubPublisher implements MessagePublisherPort, AutoCloseable {

    private final PubSubSettings settings;
    private final PubSubTransport transport;
    private final Publisher publisher;

    GooglePubSubPublisher(PubSubSettings settings, PubSubTransport transport, Publisher publisher) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
    }

    public static GooglePubSubPublisher connect(PubSubSettings settings) {
        PubSubTransport transport = PubSubTransport.open(settings);
        try {
            if (settings.usesEmulator()) {
                transport.ensureTopicAndPullSubscription();
            }
            Publisher.Builder builder = Publisher.newBuilder(transport.topicName());
            if (transport.channelProvider() != null) {
                builder.setChannelProvider(transport.channelProvider())
                        .setCredentialsProvider(transport.credentialsProvider());
            }
            return new GooglePubSubPublisher(settings, transport, builder.build());
        } catch (RuntimeException ex) {
            transport.close();
            throw translate(ex);
        } catch (Exception ex) {
            transport.close();
            throw translate(ex);
        }
    }

    public PubSubSettings settings() {
        return settings;
    }

    public Class<?> sdkClientType() {
        return publisher.getClass();
    }

    @Override
    public void publish(DeliveryInstructionMessage message) {
        Objects.requireNonNull(message, "message");
        rejectSensitive(message);
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(DeliveryInstructionJson.write(message)))
                .putAttributes("schema_version", message.schemaVersion())
                .putAttributes("instruction_id", message.instructionId())
                .putAttributes("integration_id", message.integrationId())
                .putAttributes("destination_system", message.destinationSystem())
                .build();
        try {
            await(publisher.publish(pubsubMessage));
        } catch (RuntimeException ex) {
            throw translate(ex);
        }
    }

    @Override
    public void close() {
        try {
            publisher.shutdown();
            publisher.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            transport.close();
        }
    }

    private static void rejectSensitive(DeliveryInstructionMessage message) {
        String payload = message.instruction().payloadJson();
        String upper = payload == null ? "" : payload.toUpperCase();
        if (upper.contains("BEGIN PRIVATE")
                || upper.contains("AUTHORIZATION")
                || upper.contains("BEARER ")
                || upper.contains("CLIENT_SECRET")) {
            throw new MessagePublishException(PublishFailureKind.PERMANENT, "FORBIDDEN_MESSAGE_MATERIAL");
        }
    }

    private static <T> T await(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MessagePublishException(PublishFailureKind.TRANSIENT, "PUBLISH_INTERRUPTED");
        } catch (ExecutionException ex) {
            throw translate(ex.getCause());
        }
    }

    private static RuntimeException translate(Throwable cause) {
        PubSubAccessException wrapped = PubSubAccessException.wrap(cause);
        PublishFailureKind kind = wrapped.kind() == PubSubAccessException.Kind.UNAVAILABLE
                ? PublishFailureKind.TRANSIENT
                : PublishFailureKind.PERMANENT;
        return new MessagePublishException(kind, wrapped.getMessage());
    }
}
