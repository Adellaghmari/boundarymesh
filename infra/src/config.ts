export const PHASE_15A_RESOURCE_GUARD = "ZERO_RESOURCES" as const;

export const TARGET_REGION = "europe-west4" as const;
export const DEV_ENVIRONMENT = "dev" as const;
export const DEV_GCP_PROJECT = "boundarymesh-dev" as const;

export type EnvironmentName = typeof DEV_ENVIRONMENT;
export type TargetRegion = typeof TARGET_REGION;

export interface FoundationConfig {
    readonly environment: EnvironmentName;
    readonly region: TargetRegion;
    readonly gcpProjectId: string;
}

export function loadFoundationConfig(input: FoundationConfig): FoundationConfig {
    if (input.environment !== DEV_ENVIRONMENT) {
        throw new Error("Phase 15B only documents the dev environment.");
    }
    if (input.region !== TARGET_REGION) {
        throw new Error(`Region must be ${TARGET_REGION}.`);
    }
    if (input.gcpProjectId.trim() !== DEV_GCP_PROJECT) {
        throw new Error(`gcpProjectId must be ${DEV_GCP_PROJECT}.`);
    }
    return {
        environment: input.environment,
        region: input.region,
        gcpProjectId: DEV_GCP_PROJECT,
    };
}

export const FOUNDATION: FoundationConfig = loadFoundationConfig({
    environment: DEV_ENVIRONMENT,
    region: TARGET_REGION,
    gcpProjectId: DEV_GCP_PROJECT,
});
