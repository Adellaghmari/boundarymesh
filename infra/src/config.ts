export const PHASE_15A_RESOURCE_GUARD = "ZERO_RESOURCES" as const;

export const TARGET_REGION = "europe-west4" as const;
export const DEV_ENVIRONMENT = "dev" as const;
export const GCP_PROJECT_PLACEHOLDER = "REPLACE_IN_PHASE_15B" as const;

export type EnvironmentName = typeof DEV_ENVIRONMENT;
export type TargetRegion = typeof TARGET_REGION;

export interface FoundationConfig {
    readonly environment: EnvironmentName;
    readonly region: TargetRegion;
    readonly gcpProjectId: string;
}

export function loadFoundationConfig(input: FoundationConfig): FoundationConfig {
    if (input.environment !== DEV_ENVIRONMENT) {
        throw new Error("Phase 15A only documents the dev environment.");
    }
    if (input.region !== TARGET_REGION) {
        throw new Error(`Region must be ${TARGET_REGION}.`);
    }
    if (input.gcpProjectId.trim() === "") {
        throw new Error("gcpProjectId must be present as a Phase 15B placeholder.");
    }
    return {
        environment: input.environment,
        region: input.region,
        gcpProjectId: input.gcpProjectId.trim(),
    };
}

export const FOUNDATION: FoundationConfig = loadFoundationConfig({
    environment: DEV_ENVIRONMENT,
    region: TARGET_REGION,
    gcpProjectId: GCP_PROJECT_PLACEHOLDER,
});
