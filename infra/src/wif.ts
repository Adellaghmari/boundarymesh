import * as gcp from "@pulumi/gcp";
import type { FoundationConfig } from "./config";

export const GITHUB_REPOSITORY_ID = "1385415640" as const;
export const GITHUB_REPOSITORY_OWNER_ID = "179922674" as const;
export const GCP_PROJECT_NUMBER = "891977254468" as const;
export const GITHUB_WIF_POOL_ID = "boundarymesh-github" as const;
export const GITHUB_WIF_PROVIDER_ID = "github-actions" as const;
export const GITHUB_VERIFICATION_ENVIRONMENT = "boundarymesh-dev" as const;
export const WIF_VERIFICATION_SERVICE_ACCOUNT_ID = "boundarymesh-wif-verify" as const;

export const GITHUB_OIDC_ATTRIBUTE_MAPPING = {
    "google.subject": "assertion.sub",
    "attribute.repository_id": "assertion.repository_id",
    "attribute.repository_owner_id": "assertion.repository_owner_id",
    "attribute.repository_visibility": "assertion.repository_visibility",
    "attribute.ref": "assertion.ref",
    "attribute.environment": "assertion.environment",
    "attribute.workflow_ref": "assertion.workflow_ref",
    "attribute.event_name": "assertion.event_name",
    "attribute.repository": "assertion.repository",
} as const;

export const GITHUB_OIDC_ATTRIBUTE_CONDITION = [
    `assertion.repository_id == '${GITHUB_REPOSITORY_ID}'`,
    `assertion.repository_owner_id == '${GITHUB_REPOSITORY_OWNER_ID}'`,
    "assertion.repository_visibility == 'private'",
    "assertion.ref == 'refs/heads/main'",
    "assertion.environment == 'boundarymesh-dev'",
    "assertion.workflow_ref == " +
        "'Adellaghmari/boundarymesh/.github/workflows/wif-verify.yml@refs/heads/main'",
    "assertion.event_name == 'workflow_dispatch'",
].join(" && ");

export function defineGitHubWifFoundation(foundation: FoundationConfig) {
    const iamApi = new gcp.projects.Service("wif-iam-api", {
        project: foundation.gcpProjectId,
        service: "iam.googleapis.com",
        disableOnDestroy: false,
    });
    const iamCredentialsApi = new gcp.projects.Service("wif-iam-credentials-api", {
        project: foundation.gcpProjectId,
        service: "iamcredentials.googleapis.com",
        disableOnDestroy: false,
    });
    const stsApi = new gcp.projects.Service("wif-sts-api", {
        project: foundation.gcpProjectId,
        service: "sts.googleapis.com",
        disableOnDestroy: false,
    });
    const requiredApis = [iamApi, iamCredentialsApi, stsApi];

    const pool = new gcp.iam.WorkloadIdentityPool(
        "github-workload-identity-pool",
        {
            project: foundation.gcpProjectId,
            workloadIdentityPoolId: GITHUB_WIF_POOL_ID,
            displayName: "BoundaryMesh GitHub Actions",
            description: "Trust GitHub Actions for the BoundaryMesh repository",
        },
        { dependsOn: requiredApis },
    );

    const provider = new gcp.iam.WorkloadIdentityPoolProvider(
        "github-oidc-provider",
        {
            project: foundation.gcpProjectId,
            workloadIdentityPoolId: pool.workloadIdentityPoolId,
            workloadIdentityPoolProviderId: GITHUB_WIF_PROVIDER_ID,
            displayName: "BoundaryMesh GitHub OIDC",
            description: "Strict GitHub OIDC trust for the dormant BoundaryMesh verification workflow",
            attributeMapping: GITHUB_OIDC_ATTRIBUTE_MAPPING,
            attributeCondition: GITHUB_OIDC_ATTRIBUTE_CONDITION,
            oidc: { issuerUri: "https://token.actions.githubusercontent.com" },
        },
        { dependsOn: requiredApis },
    );

    const verificationServiceAccount = new gcp.serviceaccount.Account(
        "wif-verification-service-account",
        {
            project: foundation.gcpProjectId,
            accountId: WIF_VERIFICATION_SERVICE_ACCOUNT_ID,
            displayName: "BoundaryMesh WIF Verification",
            description: "Short-lived GitHub OIDC federation verification only",
            createIgnoreAlreadyExists: false,
        },
        { dependsOn: [iamApi] },
    );

    const repositoryPrincipal =
        `principalSet://iam.googleapis.com/projects/${GCP_PROJECT_NUMBER}/locations/global/` +
        `workloadIdentityPools/${GITHUB_WIF_POOL_ID}/attribute.repository_id/${GITHUB_REPOSITORY_ID}`;
    const verificationImpersonation = new gcp.serviceaccount.IAMMember(
        "wif-verification-impersonation",
        {
            serviceAccountId: verificationServiceAccount.name,
            role: "roles/iam.workloadIdentityUser",
            member: repositoryPrincipal,
        },
        { dependsOn: [provider] },
    );

    const environmentPrincipal =
        `principalSet://iam.googleapis.com/projects/${GCP_PROJECT_NUMBER}/locations/global/` +
        `workloadIdentityPools/${GITHUB_WIF_POOL_ID}/attribute.environment/${GITHUB_VERIFICATION_ENVIRONMENT}`;
    const verificationEnvironmentImpersonation = new gcp.serviceaccount.IAMMember(
        "wif-verification-environment-impersonation",
        {
            serviceAccountId: verificationServiceAccount.name,
            role: "roles/iam.workloadIdentityUser",
            member: environmentPrincipal,
        },
        { dependsOn: [provider] },
    );

    return {
        iamApi,
        iamCredentialsApi,
        stsApi,
        pool,
        provider,
        verificationServiceAccount,
        verificationImpersonation,
        verificationEnvironmentImpersonation,
    };
}
