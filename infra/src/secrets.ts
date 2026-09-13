import * as gcp from "@pulumi/gcp";
import type { FoundationConfig } from "./config";

/**
 * Phase 15E — Secret Manager containers only.
 *
 * Creates the Secret Manager API and four empty secret IDs. No version
 * resources, no payloads, no IAM, no service accounts, no workload grants.
 *
 * Replication is user-managed in europe-west4 to match the project region.
 */
export const PHASE_15E_SECRET_IDS = [
    "hmac-quayforge",
    "covecrm-api",
    "lumenway-api",
    "rivertide-api",
] as const;

const SECRET_DESCRIPTIONS: Record<(typeof PHASE_15E_SECRET_IDS)[number], string> = {
    "hmac-quayforge": "QuayForge webhook HMAC container. No version in this phase.",
    "covecrm-api": "CoveCRM connector credential container. Worker-accessed later.",
    "lumenway-api": "Lumenway connector credential container. Worker-accessed later.",
    "rivertide-api": "Rivertide connector credential container. Worker-accessed later.",
};

export function defineSecretManager(foundation: FoundationConfig) {
    const secretManagerApi = new gcp.projects.Service("secret-manager-api", {
        project: foundation.gcpProjectId,
        service: "secretmanager.googleapis.com",
        disableOnDestroy: false,
    });

    const replication = {
        userManaged: {
            replicas: [{ location: foundation.region }],
        },
    };

    const secrets = Object.fromEntries(
        PHASE_15E_SECRET_IDS.map((secretId) => [
            secretId,
            new gcp.secretmanager.Secret(
                secretId,
                {
                    project: foundation.gcpProjectId,
                    secretId,
                    replication,
                    labels: {
                        purpose: "application-secret-container",
                        system: "boundarymesh",
                    },
                    annotations: {
                        description: SECRET_DESCRIPTIONS[secretId],
                    },
                },
                { dependsOn: [secretManagerApi] },
            ),
        ]),
    ) as Record<(typeof PHASE_15E_SECRET_IDS)[number], gcp.secretmanager.Secret>;

    return { secretManagerApi, secrets };
}
