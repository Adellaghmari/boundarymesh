/**
 * Phase 15A — Pulumi TypeScript repository foundation.
 *
 * This program loads typed non-secret configuration only. It must not
 * instantiate CustomResource, ComponentResource, or any @pulumi/gcp type.
 * Do not run pulumi preview or pulumi up in this phase.
 */
import {
    FOUNDATION,
    PHASE_15A_RESOURCE_GUARD,
    type FoundationConfig,
} from "./config.js";

export const phase15AResourceGuard: typeof PHASE_15A_RESOURCE_GUARD = PHASE_15A_RESOURCE_GUARD;

export const foundation: FoundationConfig = FOUNDATION;

export const environment = foundation.environment;
export const region = foundation.region;
export const gcpProjectId = foundation.gcpProjectId;
