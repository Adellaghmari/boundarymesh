/**
 * Phase 15E — Artifact Registry plus empty Secret Manager containers.
 * First Secret Manager apply completed. Do not add versions, IAM, or workloads.
 */
import * as pulumi from "@pulumi/pulumi";
import {
    loadFoundationConfig,
    type EnvironmentName,
    type FoundationConfig,
    type TargetRegion,
} from "./config";
import { defineArtifactRegistry } from "./registry";
import { defineSecretManager, PHASE_15E_SECRET_IDS } from "./secrets";

const gcpConfig = new pulumi.Config("gcp");
const appConfig = new pulumi.Config();

export const foundation: FoundationConfig = loadFoundationConfig({
    environment: appConfig.require("environment") as EnvironmentName,
    region: gcpConfig.require("region") as TargetRegion,
    gcpProjectId: gcpConfig.require("project"),
});

const registry = defineArtifactRegistry(foundation);
const secretManager = defineSecretManager(foundation);

export const phase15DScope = "ARTIFACT_REGISTRY_ONLY" as const;
export const phase15EScope = "SECRET_CONTAINERS_ONLY" as const;

export const environment = foundation.environment;
export const region = foundation.region;
export const gcpProjectId = foundation.gcpProjectId;
export const artifactRegistryRepositoryId = registry.repository.repositoryId;
export const artifactRegistryLocation = registry.repository.location;
export const artifactRegistryFormat = registry.repository.format;
export const secretManagerSecretIds = PHASE_15E_SECRET_IDS;
export const secretManagerApiService = secretManager.secretManagerApi.service;
