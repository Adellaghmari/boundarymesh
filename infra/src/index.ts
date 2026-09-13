/**
 * Phase 15F-B — Artifact Registry, empty Secret Manager containers, and one empty
 * named Firestore Native database. Do not write application documents here.
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
const firestore = defineFirestore(foundation);

export const phase15DScope = "ARTIFACT_REGISTRY_ONLY" as const;
export const phase15EScope = "SECRET_CONTAINERS_ONLY" as const;
export const phase15FScope = "FIRESTORE_DATABASE_PREVIEW" as const;

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
