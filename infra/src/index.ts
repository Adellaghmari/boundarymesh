/**
 * Phase 15D — Artifact Registry definition. First controlled apply completed
 * for Artifact Registry only. Do not add other GCP resources here.
 *
 * Allowed GCP resources: artifactregistry.googleapis.com enablement and one
 * Docker Artifact Registry repository. No workloads.
 */
import * as pulumi from "@pulumi/pulumi";
import {
    loadFoundationConfig,
    type EnvironmentName,
    type FoundationConfig,
    type TargetRegion,
} from "./config";
import { defineArtifactRegistry } from "./registry";

const gcpConfig = new pulumi.Config("gcp");
const appConfig = new pulumi.Config();

export const foundation: FoundationConfig = loadFoundationConfig({
    environment: appConfig.require("environment") as EnvironmentName,
    region: gcpConfig.require("region") as TargetRegion,
    gcpProjectId: gcpConfig.require("project"),
});

const registry = defineArtifactRegistry(foundation);

export const phase15DScope = "ARTIFACT_REGISTRY_ONLY" as const;

export const environment = foundation.environment;
export const region = foundation.region;
export const gcpProjectId = foundation.gcpProjectId;
export const artifactRegistryRepositoryId = registry.repository.repositoryId;
export const artifactRegistryLocation = registry.repository.location;
export const artifactRegistryFormat = registry.repository.format;
