/**
 * Phase 15H-B — Artifact Registry, empty Secret Manager containers, one named
 * Firestore Native database, Pub/Sub delivery topology, and Cloud KMS
 * attestation signing key (applied).
 */
import * as pulumi from "@pulumi/pulumi";
import {
    loadFoundationConfig,
    type EnvironmentName,
    type FoundationConfig,
    type TargetRegion,
} from "./config";
import { defineArtifactRegistry } from "./registry";
import { defineFirestore } from "./firestore";
import { defineKms } from "./kms";
import { definePubSub } from "./pubsub";
import { defineSecretManager, PHASE_15E_SECRET_IDS } from "./secrets";
import {
    defineSharedStateFoundation,
    PULUMI_SECRETS_CRYPTO_KEY_NAME,
    PULUMI_SECRETS_KEY_RING_NAME,
    PULUMI_STATE_BACKEND_URL,
} from "./shared-state";
import { defineWorkloads } from "./workloads";
import { defineWebEdge } from "./web-edge";
import { defineGitHubWifFoundation } from "./wif";

const gcpConfig = new pulumi.Config("gcp");
const appConfig = new pulumi.Config();

export const foundation: FoundationConfig = loadFoundationConfig({
    environment: appConfig.require("environment") as EnvironmentName,
    region: gcpConfig.require("region") as TargetRegion,
    gcpProjectId: gcpConfig.require("project"),
});

const registry = defineArtifactRegistry(foundation);
const secretManager = defineSecretManager(foundation);
const firestore = defineFirestore(foundation);
const pubsub = definePubSub(foundation);
const kms = defineKms(foundation);
const sharedState = defineSharedStateFoundation(foundation, kms.kmsApi);
const workloads = defineWorkloads(foundation, {
    deliveryTopic: pubsub.deliveryTopic,
    deliveryWorkerSubscription: pubsub.deliveryWorkerSubscription,
    cryptoKey: kms.cryptoKey,
    secrets: secretManager.secrets,
});
const webEdge = defineWebEdge(foundation, workloads.apiService, workloads.runApi);
const githubWif = defineGitHubWifFoundation(foundation);

export const phase15DScope = "ARTIFACT_REGISTRY_ONLY" as const;
export const phase15EScope = "SECRET_CONTAINERS_ONLY" as const;
export const phase15FScope = "FIRESTORE_DATABASE_PREVIEW" as const;
export const phase15GScope = "PUBSUB_PREVIEW" as const;
// Historical implementation-scope marker introduced for the 15H-A preview.
// This literal is not deployment status: 15H-B applied the three KMS resources.
// Keep it stable; an informational rename does not warrant a stack update.
export const phase15HScope = "KMS_PREVIEW" as const;
export const phase15IScope = "WORKLOAD_COMPOSITION_PREVIEW_ONLY" as const;

export const environment = foundation.environment;
export const region = foundation.region;
export const gcpProjectId = foundation.gcpProjectId;
export const artifactRegistryRepositoryId = registry.repository.repositoryId;
export const artifactRegistryLocation = registry.repository.location;
export const artifactRegistryFormat = registry.repository.format;
export const secretManagerSecretIds = PHASE_15E_SECRET_IDS;
export const secretManagerApiService = secretManager.secretManagerApi.service;
export const firestoreApiService = firestore.firestoreApi.service;
export const firestoreDatabaseId = firestore.database.name;
export const firestoreDatabaseLocation = firestore.database.locationId;
export const firestoreDatabaseType = firestore.database.type;
export const pubsubApiService = pubsub.pubsubApi.service;
export const deliveryTopicName = pubsub.deliveryTopic.name;
export const deliveryWorkerSubscriptionName = pubsub.deliveryWorkerSubscription.name;
export const deliveryDlqTopicName = pubsub.deliveryDlqTopic.name;
export const kmsApiService = kms.kmsApi.service;
export const attestationKeyRingName = kms.keyRing.name;
export const attestationCryptoKeyName = kms.cryptoKey.name;
export const pulumiStateBucketName = sharedState.stateBucket.name;
export const pulumiStateBackendUrl = PULUMI_STATE_BACKEND_URL;
export const pulumiSecretsKeyRingName = PULUMI_SECRETS_KEY_RING_NAME;
export const pulumiSecretsCryptoKeyName = PULUMI_SECRETS_CRYPTO_KEY_NAME;
export const pulumiSecretsProviderUri =
    `gcpkms://projects/${foundation.gcpProjectId}/locations/${foundation.region}` +
    `/keyRings/${PULUMI_SECRETS_KEY_RING_NAME}/cryptoKeys/${PULUMI_SECRETS_CRYPTO_KEY_NAME}`;
export const cloudRunApiService = workloads.runApi.service;
export const apiServiceAccountEmail = workloads.apiServiceAccount.email;
export const workerServiceAccountEmail = workloads.workerServiceAccount.email;
export const apiCloudRunServiceName = workloads.apiService.name;
export const workerPoolName = workloads.workerPool.name;
export const webEdgeServiceName = webEdge.service.name;
export const webEdgeServiceAccountEmail = webEdge.identity.email;
export const githubWorkloadIdentityPoolName = githubWif.pool.name;
export const githubWorkloadIdentityProviderName = githubWif.provider.name;
export const wifVerificationServiceAccountEmail = githubWif.verificationServiceAccount.email;
