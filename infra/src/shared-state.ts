import * as gcp from "@pulumi/gcp";
import type * as pulumi from "@pulumi/pulumi";
import type { FoundationConfig } from "./config";

export const PULUMI_STATE_BUCKET_NAME = "bm-pulumi-state-eu4-21cad20b325f";
export const PULUMI_STATE_BACKEND_URL = `gs://${PULUMI_STATE_BUCKET_NAME}`;
export const PULUMI_SECRETS_KEY_RING_NAME = "boundarymesh-pulumi";
export const PULUMI_SECRETS_CRYPTO_KEY_NAME = "state-secrets";
export const PULUMI_SECRETS_ROTATION_PERIOD = "7776000s";

/**
 * Phase 16C shared-state foundation. These resources prepare a recoverable GCS
 * backend and a separate symmetric KMS key for a later, explicitly controlled
 * migration. This module grants no access and does not migrate the active stack.
 */
export function defineSharedStateFoundation(
    foundation: FoundationConfig,
    kmsApi: pulumi.Resource,
) {
    const stateBucket = new gcp.storage.Bucket(
        "pulumi-state-bucket",
        {
            name: PULUMI_STATE_BUCKET_NAME,
            project: foundation.gcpProjectId,
            location: foundation.region,
            storageClass: "STANDARD",
            uniformBucketLevelAccess: true,
            publicAccessPrevention: "enforced",
            versioning: { enabled: true },
            forceDestroy: false,
            lifecycleRules: [
                {
                    action: { type: "Delete" },
                    condition: {
                        daysSinceNoncurrentTime: 90,
                        numNewerVersions: 10,
                    },
                },
            ],
        },
        { protect: true },
    );

    const secretsKeyRing = new gcp.kms.KeyRing(
        "pulumi-secrets-key-ring",
        {
            name: PULUMI_SECRETS_KEY_RING_NAME,
            project: foundation.gcpProjectId,
            location: foundation.region,
        },
        { dependsOn: [kmsApi], protect: true },
    );

    const secretsCryptoKey = new gcp.kms.CryptoKey(
        "pulumi-secrets-crypto-key",
        {
            name: PULUMI_SECRETS_CRYPTO_KEY_NAME,
            keyRing: secretsKeyRing.id,
            purpose: "ENCRYPT_DECRYPT",
            rotationPeriod: PULUMI_SECRETS_ROTATION_PERIOD,
            versionTemplate: {
                algorithm: "GOOGLE_SYMMETRIC_ENCRYPTION",
                protectionLevel: "SOFTWARE",
            },
        },
        { dependsOn: [secretsKeyRing], protect: true },
    );

    return { stateBucket, secretsKeyRing, secretsCryptoKey };
}

