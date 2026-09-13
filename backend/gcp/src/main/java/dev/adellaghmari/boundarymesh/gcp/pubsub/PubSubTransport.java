package dev.adellaghmari.boundarymesh.gcp.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Shared emulator/production transport. Production uses ADC; emulator uses plaintext + no credentials. */
final class PubSubTransport implements AutoCloseable {

    private final PubSubSettings settings;
    private final ManagedChannel channel;
    private final TransportChannelProvider channelProvider;
    private final CredentialsProvider credentialsProvider;

    private PubSubTransport(
            PubSubSettings settings,
            ManagedChannel channel,
            TransportChannelProvider channelProvider,
            CredentialsProvider credentialsProvider) {
        this.settings = settings;
        this.channel = channel;
        this.channelProvider = channelProvider;
        this.credentialsProvider = credentialsProvider;
    }

    static PubSubTransport open(PubSubSettings settings) {
        if (settings.usesEmulator()) {
            ManagedChannel channel = ManagedChannelBuilder.forTarget(
                            settings.emulatorHost().orElseThrow())
                    .usePlaintext()
                    .build();
            return new PubSubTransport(
                    settings,
                    channel,
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel)),
                    NoCredentialsProvider.create());
        }
        return new PubSubTransport(settings, null, null, null);
    }

    PubSubSettings settings() {
        return settings;
    }

    TransportChannelProvider channelProvider() {
        return channelProvider;
    }

    CredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    TopicName topicName() {
        return TopicName.of(settings.projectId(), settings.topicId());
    }

    SubscriptionName subscriptionName() {
        return SubscriptionName.of(settings.projectId(), settings.subscriptionId());
    }

    void ensureTopicAndPullSubscription() {
        if (!settings.usesEmulator()) {
            return;
        }
        ManagedChannel admin = ManagedChannelBuilder.forTarget(
                        settings.emulatorHost().orElseThrow())
                .usePlaintext()
                .build();
        try {
            TransportChannelProvider adminChannel =
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(admin));
            CredentialsProvider credentials = NoCredentialsProvider.create();
            TopicAdminSettings topicSettings = TopicAdminSettings.newBuilder()
                    .setTransportChannelProvider(adminChannel)
                    .setCredentialsProvider(credentials)
                    .build();
            SubscriptionAdminSettings subscriptionSettings = SubscriptionAdminSettings.newBuilder()
                    .setTransportChannelProvider(adminChannel)
                    .setCredentialsProvider(credentials)
                    .build();
            try (TopicAdminClient topics = TopicAdminClient.create(topicSettings);
                    SubscriptionAdminClient subscriptions = SubscriptionAdminClient.create(subscriptionSettings)) {
                TopicName topic = topicName();
                try {
                    topics.getTopic(topic);
                } catch (RuntimeException ex) {
                    topics.createTopic(
                            Topic.newBuilder().setName(topic.toString()).build());
                }
                SubscriptionName subscription = subscriptionName();
                try {
                    subscriptions.getSubscription(subscription);
                } catch (RuntimeException ex) {
                    subscriptions.createSubscription(Subscription.newBuilder()
                            .setName(subscription.toString())
                            .setTopic(topic.toString())
                            .setAckDeadlineSeconds(60)
                            .setPushConfig(PushConfig.getDefaultInstance())
                            .build());
                }
            }
        } catch (IOException ex) {
            throw PubSubAccessException.wrap(ex);
        } finally {
            admin.shutdownNow();
        }
    }

    @Override
    public void close() {
        if (channel == null) {
            return;
        }
        channel.shutdown();
        try {
            channel.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
        }
    }
}
