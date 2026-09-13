package dev.adellaghmari.boundarymesh.gcp.pubsub;

import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;
import dev.adellaghmari.boundarymesh.application.outbox.ConsumeDisposition;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryInstructionHandler;
import dev.adellaghmari.boundarymesh.application.outbox.DeliveryInstructionMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pull consumer for the future Cloud Run Worker Pool. No public HTTP endpoint. Not deployed in
 * Phase 15G-A.
 */
public final class GooglePubSubPullConsumer implements AutoCloseable {

    private final PubSubSettings settings;
    private final PubSubTransport transport;
    private final GrpcSubscriberStub stub;
    private final DeliveryInstructionHandler handler;

    GooglePubSubPullConsumer(
            PubSubSettings settings,
            PubSubTransport transport,
            GrpcSubscriberStub stub,
            DeliveryInstructionHandler handler) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.stub = Objects.requireNonNull(stub, "stub");
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    public static GooglePubSubPullConsumer connect(PubSubSettings settings, DeliveryInstructionHandler handler) {
        PubSubTransport transport = PubSubTransport.open(settings);
        try {
            if (settings.usesEmulator()) {
                transport.ensureTopicAndPullSubscription();
            }
            SubscriberStubSettings.Builder stubSettings = SubscriberStubSettings.newBuilder();
            if (transport.channelProvider() != null) {
                stubSettings
                        .setTransportChannelProvider(transport.channelProvider())
                        .setCredentialsProvider(transport.credentialsProvider());
            }
            return new GooglePubSubPullConsumer(
                    settings, transport, GrpcSubscriberStub.create(stubSettings.build()), handler);
        } catch (IOException | RuntimeException ex) {
            transport.close();
            throw PubSubAccessException.wrap(ex);
        }
    }

    public PubSubSettings settings() {
        return settings;
    }

    public Class<?> sdkClientType() {
        return stub.getClass();
    }

    /**
     * Lease then NACK without handling. Live tests use this to detect unexpected unread messages
     * without acknowledging them.
     */
    public int nackUnprocessed(int maxMessages) {
        PullRequest request = PullRequest.newBuilder()
                .setSubscription(transport.subscriptionName().toString())
                .setMaxMessages(Math.max(1, maxMessages))
                .build();
        PullResponse response;
        try {
            response = stub.pullCallable().call(request);
        } catch (RuntimeException ex) {
            throw PubSubAccessException.wrap(ex);
        }
        for (ReceivedMessage received : response.getReceivedMessagesList()) {
            nack(received.getAckId());
        }
        return response.getReceivedMessagesCount();
    }

    /**
     * ACK only payloads that belong to the current live run. Foreign messages are NACKed and
     * counted so they are not silently consumed.
     */
    public int ackOwnedNackForeign(int maxMessages, java.util.function.Predicate<String> owned) {
        Objects.requireNonNull(owned, "owned");
        PullRequest request = PullRequest.newBuilder()
                .setSubscription(transport.subscriptionName().toString())
                .setMaxMessages(Math.max(1, maxMessages))
                .build();
        PullResponse response;
        try {
            response = stub.pullCallable().call(request);
        } catch (RuntimeException ex) {
            throw PubSubAccessException.wrap(ex);
        }
        int foreign = 0;
        for (ReceivedMessage received : response.getReceivedMessagesList()) {
            String payload = received.getMessage().getData().toStringUtf8()
                    + received.getMessage().getAttributesMap();
            if (owned.test(payload)) {
                ack(received.getAckId());
            } else {
                nack(received.getAckId());
                foreign++;
            }
        }
        return foreign;
    }

    public List<ConsumeDisposition> pullAndHandle(int maxMessages) {
        PullRequest request = PullRequest.newBuilder()
                .setSubscription(transport.subscriptionName().toString())
                .setMaxMessages(Math.max(1, maxMessages))
                .build();
        PullResponse response;
        try {
            response = stub.pullCallable().call(request);
        } catch (RuntimeException ex) {
            throw PubSubAccessException.wrap(ex);
        }
        List<ConsumeDisposition> dispositions = new ArrayList<>();
        for (ReceivedMessage received : response.getReceivedMessagesList()) {
            ConsumeDisposition disposition = handle(received);
            dispositions.add(disposition);
            if (disposition == ConsumeDisposition.ACK) {
                ack(received.getAckId());
            } else {
                nack(received.getAckId());
            }
        }
        return List.copyOf(dispositions);
    }

    private ConsumeDisposition handle(ReceivedMessage received) {
        try {
            String json = received.getMessage().getData().toStringUtf8();
            DeliveryInstructionMessage message = DeliveryInstructionJson.read(json);
            return handler.handle(message);
        } catch (RuntimeException ex) {
            return ConsumeDisposition.ACK;
        }
    }

    private void ack(String ackId) {
        UnaryCallable<AcknowledgeRequest, com.google.protobuf.Empty> callable = stub.acknowledgeCallable();
        callable.call(AcknowledgeRequest.newBuilder()
                .setSubscription(transport.subscriptionName().toString())
                .addAckIds(ackId)
                .build());
    }

    private void nack(String ackId) {
        stub.modifyAckDeadlineCallable()
                .call(ModifyAckDeadlineRequest.newBuilder()
                        .setSubscription(transport.subscriptionName().toString())
                        .addAckIds(ackId)
                        .setAckDeadlineSeconds(0)
                        .build());
    }

    @Override
    public void close() {
        try {
            stub.close();
        } finally {
            transport.close();
        }
    }
}
