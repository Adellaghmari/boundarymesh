import * as gcp from "@pulumi/gcp";
import type { FoundationConfig } from "./config";

/**
 * Phase 15D — Artifact Registry only. Do not add Cloud Run, Firestore, Pub/Sub,
 * Secret Manager, KMS, IAM, or other application resources here.
 */
export function defineArtifactRegistry(foundation: FoundationConfig) {
    const artifactRegistryApi = new gcp.projects.Service("artifact-registry-api", {
        project: foundation.gcpProjectId,
        service: "artifactregistry.googleapis.com",
        disableOnDestroy: false,
    });

    const repository = new gcp.artifactregistry.Repository(
        "boundarymesh-images",
        {
            project: foundation.gcpProjectId,
            location: foundation.region,
            repositoryId: "boundarymesh",
            format: "DOCKER",
            description: "BoundaryMesh OCI application images",
            mode: "STANDARD_REPOSITORY",
        },
        { dependsOn: [artifactRegistryApi] },
    );

    return { artifactRegistryApi, repository };
}
