import * as gcp from "@pulumi/gcp";
import type { FoundationConfig } from "./config";

export const API_SERVICE_ACCOUNT_ID = "boundarymesh-api" as const;
export const WORKER_SERVICE_ACCOUNT_ID = "boundarymesh-worker" as const;

export const PHASE_15I_C1_API_IMAGE =
    "europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/" +
    "boundarymesh-api@sha256:28e47427709d43a1a12b363cc28b7a72e6250ea966d4ecb89f8da0108e980529";
export const PHASE_15I_C1_WORKER_IMAGE =
    "europe-west4-docker.pkg.dev/boundarymesh-dev/boundarymesh/" +
    "boundarymesh-worker@sha256:a031fd689c80faef122b2eb7a35c35c6cb04035f333c493e5d7decea1d08b76a";

interface WorkloadDependencies {
    readonly deliveryTopic: gcp.pubsub.Topic;
    readonly deliveryWorkerSubscription: gcp.pubsub.Subscription;
    readonly cryptoKey: gcp.kms.CryptoKey;
    readonly secrets: Record<string, gcp.secretmanager.Secret>;
}

const baseEnvironment = (foundation: FoundationConfig) => [
    { name: "SPRING_PROFILES_ACTIVE", value: "prod" },
    { name: "BOUNDARYMESH_GCP_PROJECT", value: foundation.gcpProjectId },
    { name: "BOUNDARYMESH_GCP_LOCATION", value: foundation.region },
    { name: "BOUNDARYMESH_FIRESTORE_DATABASE", value: "boundarymesh" },
];

const apiEnvironment = (foundation: FoundationConfig) => [
    ...baseEnvironment(foundation),
    { name: "BOUNDARYMESH_PUBSUB_TOPIC", value: "boundarymesh-deliveries" },
    { name: "BOUNDARYMESH_KMS_KEY_RING", value: "boundarymesh-attestation" },
    { name: "BOUNDARYMESH_KMS_CRYPTO_KEY", value: "exchange-attestation" },
    { name: "BOUNDARYMESH_KMS_KEY_VERSION", value: "1" },
];

const workerEnvironment = (foundation: FoundationConfig) => [
    ...baseEnvironment(foundation),
    { name: "BOUNDARYMESH_PUBSUB_TOPIC", value: "boundarymesh-deliveries" },
    { name: "BOUNDARYMESH_PUBSUB_SUBSCRIPTION", value: "boundarymesh-delivery-worker" },
];

/**
 * Phase 15I production workloads, pinned to the fresh Phase 15I-C1 registry digests. These
 * resources remain preview-only until the separately approved deployment gate.
 */
export function defineWorkloads(foundation: FoundationConfig, dependencies: WorkloadDependencies) {
    const runApi = new gcp.projects.Service("cloud-run-api", {
        project: foundation.gcpProjectId,
        service: "run.googleapis.com",
        disableOnDestroy: false,
    });

    const apiServiceAccount = new gcp.serviceaccount.Account("api-service-account", {
        project: foundation.gcpProjectId,
        accountId: API_SERVICE_ACCOUNT_ID,
        displayName: "BoundaryMesh API workload",
        description: "Keyless Cloud Run API service identity",
        createIgnoreAlreadyExists: false,
    });
    const workerServiceAccount = new gcp.serviceaccount.Account("worker-service-account", {
        project: foundation.gcpProjectId,
        accountId: WORKER_SERVICE_ACCOUNT_ID,
        displayName: "BoundaryMesh worker workload",
        description: "Keyless Cloud Run Worker Pool service identity",
        createIgnoreAlreadyExists: false,
    });

    const apiPrincipal = apiServiceAccount.email.apply((email) => `serviceAccount:${email}`);
    const workerPrincipal = workerServiceAccount.email.apply((email) => `serviceAccount:${email}`);

    const apiFirestore = new gcp.projects.IAMMember("api-firestore-data-user", {
        project: foundation.gcpProjectId,
        role: "roles/datastore.user",
        member: apiPrincipal,
    });
    const workerFirestore = new gcp.projects.IAMMember("worker-firestore-data-user", {
        project: foundation.gcpProjectId,
        role: "roles/datastore.user",
        member: workerPrincipal,
    });

    const apiPublisher = new gcp.pubsub.TopicIAMMember("api-delivery-topic-publisher", {
        project: foundation.gcpProjectId,
        topic: dependencies.deliveryTopic.name,
        role: "roles/pubsub.publisher",
        member: apiPrincipal,
    });
    const workerSubscriber = new gcp.pubsub.SubscriptionIAMMember("worker-delivery-subscription-subscriber", {
        project: foundation.gcpProjectId,
        subscription: dependencies.deliveryWorkerSubscription.name,
        role: "roles/pubsub.subscriber",
        member: workerPrincipal,
    });

    const apiKmsSigner = new gcp.kms.CryptoKeyIAMMember("api-attestation-key-signer", {
        cryptoKeyId: dependencies.cryptoKey.id,
        role: "roles/cloudkms.signer",
        member: apiPrincipal,
    });
    const apiKmsPublicKeyViewer = new gcp.kms.CryptoKeyIAMMember("api-attestation-public-key-viewer", {
        cryptoKeyId: dependencies.cryptoKey.id,
        role: "roles/cloudkms.publicKeyViewer",
        member: apiPrincipal,
    });

    const apiWebhookSecret = new gcp.secretmanager.SecretIamMember("api-hmac-quayforge-accessor", {
        project: foundation.gcpProjectId,
        secretId: dependencies.secrets["hmac-quayforge"].id,
        role: "roles/secretmanager.secretAccessor",
        member: apiPrincipal,
    });
    const workerSecretAccess = ["covecrm-api", "lumenway-api", "rivertide-api"].map(
        (secretId) =>
            new gcp.secretmanager.SecretIamMember(`worker-${secretId}-accessor`, {
                project: foundation.gcpProjectId,
                secretId: dependencies.secrets[secretId].id,
                role: "roles/secretmanager.secretAccessor",
                member: workerPrincipal,
            }),
    );

    const apiService = new gcp.cloudrunv2.Service(
        "api-cloud-run-service",
        {
            project: foundation.gcpProjectId,
            name: "boundarymesh-api",
            location: foundation.region,
            description: "BoundaryMesh synchronous enforcement API",
            deletionProtection: true,
            ingress: "INGRESS_TRAFFIC_ALL",
            invokerIamDisabled: false,
            scaling: {
                scalingMode: "AUTOMATIC",
                minInstanceCount: 0,
                maxInstanceCount: 2,
            },
            template: {
                serviceAccount: apiServiceAccount.email,
                maxInstanceRequestConcurrency: 20,
                containers: [
                    {
                        name: "api",
                        image: PHASE_15I_C1_API_IMAGE,
                        ports: { name: "http1", containerPort: 8080 },
                        envs: [...apiEnvironment(foundation), { name: "BOUNDARYMESH_RUNTIME_MODE", value: "GCP" }],
                        resources: {
                            limits: { cpu: "1", memory: "512Mi" },
                            cpuIdle: true,
                            startupCpuBoost: true,
                        },
                        startupProbe: {
                            httpGet: { path: "/api/v1/health", port: 8080 },
                            periodSeconds: 5,
                            timeoutSeconds: 2,
                            failureThreshold: 24,
                        },
                        livenessProbe: {
                            httpGet: { path: "/api/v1/health", port: 8080 },
                            initialDelaySeconds: 30,
                            periodSeconds: 30,
                            timeoutSeconds: 2,
                            failureThreshold: 3,
                        },
                    },
                ],
            },
            traffics: [{ type: "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST", percent: 100 }],
            labels: { system: "boundarymesh", workload: "api", phase: "15i-c1-preview" },
        },
        {
            dependsOn: [runApi, apiFirestore, apiPublisher, apiKmsSigner, apiKmsPublicKeyViewer, apiWebhookSecret],
        },
    );

    const workerPool = new gcp.cloudrunv2.WorkerPool(
        "delivery-worker-pool",
        {
            project: foundation.gcpProjectId,
            name: "boundarymesh-worker",
            location: foundation.region,
            description: "BoundaryMesh pull-based delivery worker",
            launchStage: "GA",
            deletionProtection: true,
            scaling: { scalingMode: "MANUAL", manualInstanceCount: 1 },
            template: {
                serviceAccount: workerServiceAccount.email,
                containers: [
                    {
                        name: "worker",
                        image: PHASE_15I_C1_WORKER_IMAGE,
                        envs: workerEnvironment(foundation),
                        resources: { limits: { cpu: "1", memory: "1Gi" } },
                    },
                ],
            },
            instanceSplits: [{ type: "INSTANCE_SPLIT_ALLOCATION_TYPE_LATEST", percent: 100 }],
            labels: { system: "boundarymesh", workload: "worker", phase: "15i-c1-preview" },
        },
        { dependsOn: [runApi, workerFirestore, workerSubscriber, ...workerSecretAccess] },
    );

    return {
        runApi,
        apiServiceAccount,
        workerServiceAccount,
        apiService,
        workerPool,
        apiIam: { apiFirestore, apiPublisher, apiKmsSigner, apiKmsPublicKeyViewer, apiWebhookSecret },
        workerIam: { workerFirestore, workerSubscriber, workerSecretAccess },
    };
}
