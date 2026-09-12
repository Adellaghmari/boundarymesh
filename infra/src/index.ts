/**
 * Phase 15B — GCP project / provider validation.
 *
 * Reads non-secret Pulumi/GCP stack configuration. It must not instantiate
 * CustomResource, ComponentResource, gcp.Provider, or any @pulumi/gcp resource.
 * pulumi preview is allowed. pulumi up is forbidden.
 */
import * as pulumi from "@pulumi/pulumi";
import {
    loadFoundationConfig,
    PHASE_15A_RESOURCE_GUARD,
    type EnvironmentName,
    type FoundationConfig,
    type TargetRegion,
} from "./config";

const gcpConfig = new pulumi.Config("gcp");
const appConfig = new pulumi.Config();

export const foundation: FoundationConfig = loadFoundationConfig({
    environment: appConfig.require("environment") as EnvironmentName,
    region: gcpConfig.require("region") as TargetRegion,
    gcpProjectId: gcpConfig.require("project"),
});

export const phase15AResourceGuard: typeof PHASE_15A_RESOURCE_GUARD = PHASE_15A_RESOURCE_GUARD;

export const environment = foundation.environment;
export const region = foundation.region;
export const gcpProjectId = foundation.gcpProjectId;
