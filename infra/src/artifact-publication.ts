import * as gcp from "@pulumi/gcp";
import type { FoundationConfig } from "./config";
import {
    GCP_PROJECT_NUMBER,
    GITHUB_ARTIFACT_PUBLISH_ENVIRONMENT,
    GITHUB_WIF_POOL_ID,
} from "./wif";

// GCP service-account IDs are limited to 30 characters.
export const ARTIFACT_PUBLISHER_SERVICE_ACCOUNT_ID = "boundarymesh-artifact-publish" as const;
export const RELEASE_REPOSITORY_ID = "boundarymesh-releases" as const;
export const ARTIFACT_WRITER_ROLE = "roles/artifactregistry.writer" as const;

interface ArtifactPublicationDependencies {
    artifactRegistryApi: gcp.projects.Service;
    dockerRepository: gcp.artifactregistry.Repository;
    iamApi: gcp.projects.Service;
    wifProvider: gcp.iam.WorkloadIdentityPoolProvider;
}

/**
 * Phase 16D publication-only identity and repositories. The publisher can upload
 * to exactly two Artifact Registry repositories and has no deployment, state,
 * application-data, signing-key, or project-level authorization.
 */
export function defineArtifactPublication(
    foundation: FoundationConfig,
    dependencies: ArtifactPublicationDependencies,
) {
    const publisherServiceAccount = new gcp.serviceaccount.Account(
        "artifact-publisher-service-account",
        {
            project: foundation.gcpProjectId,
            accountId: ARTIFACT_PUBLISHER_SERVICE_ACCOUNT_ID,
            displayName: "BoundaryMesh Artifact Publisher",
            description: "Keyless immutable artifact publication only",
            createIgnoreAlreadyExists: false,
        },
        { dependsOn: [dependencies.iamApi] },
    );
    const publisherPrincipal = publisherServiceAccount.email.apply(
        (email) => `serviceAccount:${email}`,
    );

    const releaseRepository = new gcp.artifactregistry.Repository(
        "boundarymesh-releases",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            repositoryId: RELEASE_REPOSITORY_ID,
            format: "GENERIC",
            description: "Immutable BoundaryMesh frontend, manifest, SBOM, and provenance artifacts",
            mode: "STANDARD_REPOSITORY",
            deletionPolicy: "PREVENT",
        },
        { dependsOn: [dependencies.artifactRegistryApi] },
    );

    const dockerRepositoryWriter = new gcp.artifactregistry.RepositoryIamMember(
        "artifact-publisher-docker-writer",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            repository: dependencies.dockerRepository.repositoryId,
            role: ARTIFACT_WRITER_ROLE,
            member: publisherPrincipal,
        },
    );

    const releaseRepositoryWriter = new gcp.artifactregistry.RepositoryIamMember(
        "artifact-publisher-release-writer",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            repository: releaseRepository.repositoryId,
            role: ARTIFACT_WRITER_ROLE,
            member: publisherPrincipal,
        },
    );

    const publicationEnvironmentPrincipal =
        `principalSet://iam.googleapis.com/projects/${GCP_PROJECT_NUMBER}/locations/global/` +
        `workloadIdentityPools/${GITHUB_WIF_POOL_ID}/attribute.environment/${GITHUB_ARTIFACT_PUBLISH_ENVIRONMENT}`;
    const publicationEnvironmentImpersonation = new gcp.serviceaccount.IAMMember(
        "artifact-publisher-environment-impersonation",
        {
            serviceAccountId: publisherServiceAccount.name,
            role: "roles/iam.workloadIdentityUser",
            member: publicationEnvironmentPrincipal,
        },
        { dependsOn: [dependencies.wifProvider] },
    );

    return {
        publisherServiceAccount,
        releaseRepository,
        dockerRepositoryWriter,
        releaseRepositoryWriter,
        publicationEnvironmentImpersonation,
    };
}
