import * as gcp from "@pulumi/gcp";
import type { FoundationConfig } from "./config";
import { GCP_PROJECT_NUMBER, GITHUB_WIF_POOL_ID } from "./wif";

export const CI_PREVIEW_SERVICE_ACCOUNT_ID = "boundarymesh-ci-preview" as const;
export const CI_PREVIEW_ENVIRONMENT = "boundarymesh-ci-preview" as const;
export const CI_STATE_READER_ROLE_ID = "boundarymeshCiStateReader" as const;
export const CI_STATE_READER_PERMISSIONS = [
    "storage.objects.get",
    "storage.objects.list",
] as const;

interface CiPreviewDependencies {
    iamApi: gcp.projects.Service;
    stateBucket: gcp.storage.Bucket;
    stateSecretsCryptoKey: gcp.kms.CryptoKey;
    wifProvider: gcp.iam.WorkloadIdentityPoolProvider;
}

/**
 * Initial Phase 16C CI preview identity. It can read the migrated Pulumi state,
 * decrypt only that state's secrets, and be impersonated only by the future
 * environment-scoped GitHub context. The WIF provider intentionally does not
 * admit that context yet.
 */
export function defineCiPreviewIdentity(
    foundation: FoundationConfig,
    dependencies: CiPreviewDependencies,
) {
    const serviceAccount = new gcp.serviceaccount.Account(
        "boundarymesh-ci-preview",
        {
            project: foundation.gcpProjectId,
            accountId: CI_PREVIEW_SERVICE_ACCOUNT_ID,
            displayName: "BoundaryMesh CI Preview",
            description: "Keyless, non-refresh Pulumi preview identity",
            createIgnoreAlreadyExists: false,
        },
        { dependsOn: [dependencies.iamApi] },
    );
    const serviceAccountPrincipal = serviceAccount.email.apply(
        (email) => `serviceAccount:${email}`,
    );

    const stateReaderRole = new gcp.projects.IAMCustomRole(
        "boundarymeshCiStateReader",
        {
            project: foundation.gcpProjectId,
            roleId: CI_STATE_READER_ROLE_ID,
            title: "BoundaryMesh CI State Reader",
            description: "Read-only access to BoundaryMesh Pulumi state objects.",
            permissions: [...CI_STATE_READER_PERMISSIONS],
        },
        { dependsOn: [dependencies.iamApi] },
    );

    const stateBucketReader = new gcp.storage.BucketIAMMember(
        "ci-preview-state-bucket-reader",
        {
            bucket: dependencies.stateBucket.name,
            role: stateReaderRole.name,
            member: serviceAccountPrincipal,
        },
    );

    const stateSecretsDecrypter = new gcp.kms.CryptoKeyIAMMember(
        "ci-preview-state-secrets-decrypter",
        {
            cryptoKeyId: dependencies.stateSecretsCryptoKey.id,
            role: "roles/cloudkms.cryptoKeyDecrypter",
            member: serviceAccountPrincipal,
        },
    );

    const environmentPrincipal =
        `principalSet://iam.googleapis.com/projects/${GCP_PROJECT_NUMBER}/locations/global/` +
        `workloadIdentityPools/${GITHUB_WIF_POOL_ID}/attribute.environment/${CI_PREVIEW_ENVIRONMENT}`;
    const environmentImpersonation = new gcp.serviceaccount.IAMMember(
        "ci-preview-environment-impersonation",
        {
            serviceAccountId: serviceAccount.name,
            role: "roles/iam.workloadIdentityUser",
            member: environmentPrincipal,
        },
        { dependsOn: [dependencies.wifProvider] },
    );

    return {
        serviceAccount,
        stateReaderRole,
        stateBucketReader,
        stateSecretsDecrypter,
        environmentImpersonation,
    };
}
