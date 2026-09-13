import * as gcp from "@pulumi/gcp";
import type { FoundationConfig } from "./config";

/**
 * Phase 15G-A — Pub/Sub API, delivery topic, worker pull subscription, and DLQ
 * topic/inspect subscription. Preview only in this checkpoint. No IAM. Dead-letter
 * forwarding is deferred (it requires a Pub/Sub service-agent publisher binding
 * that this phase must not create).
 */
export function definePubSub(foundation: FoundationConfig) {
    const pubsubApi = new gcp.projects.Service("pubsub-api", {
        project: foundation.gcpProjectId,
        service: "pubsub.googleapis.com",
        disableOnDestroy: false,
    });

    const deliveryTopic = new gcp.pubsub.Topic(
        "delivery-topic",
        {
            project: foundation.gcpProjectId,
            name: "boundarymesh-deliveries",
        },
        { dependsOn: [pubsubApi] },
    );

    const deliveryWorkerSubscription = new gcp.pubsub.Subscription(
        "delivery-worker-subscription",
        {
            project: foundation.gcpProjectId,
            name: "boundarymesh-delivery-worker",
            topic: deliveryTopic.name,
            ackDeadlineSeconds: 60,
            retryPolicy: {
                minimumBackoff: "10s",
                maximumBackoff: "600s",
            },
        },
        { dependsOn: [deliveryTopic] },
    );

    const deliveryDlqTopic = new gcp.pubsub.Topic(
        "delivery-dlq-topic",
        {
            project: foundation.gcpProjectId,
            name: "boundarymesh-delivery-dlq",
        },
        { dependsOn: [pubsubApi] },
    );

    const deliveryDlqInspectSubscription = new gcp.pubsub.Subscription(
        "delivery-dlq-inspect-subscription",
        {
            project: foundation.gcpProjectId,
            name: "boundarymesh-delivery-dlq-inspect",
            topic: deliveryDlqTopic.name,
            ackDeadlineSeconds: 60,
        },
        { dependsOn: [deliveryDlqTopic] },
    );

    return {
        pubsubApi,
        deliveryTopic,
        deliveryWorkerSubscription,
        deliveryDlqTopic,
        deliveryDlqInspectSubscription,
    };
}
