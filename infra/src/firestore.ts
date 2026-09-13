import * as gcp from "@pulumi/gcp";
import type { FoundationConfig } from "./config";

/**
 * Phase 15F — Firestore API plus one named Native database. Applied. Application documents are
 * not managed here.
 *
 * Database ID is `boundarymesh`, never the default database. No indexes, PITR, backups,
 * or TTL policies. Existing Artifact Registry and Secret Manager resources stay unchanged.
 */
export function defineFirestore(foundation: FoundationConfig) {
    const firestoreApi = new gcp.projects.Service("firestore-api", {
        project: foundation.gcpProjectId,
        service: "firestore.googleapis.com",
        disableOnDestroy: false,
    });

    const database = new gcp.firestore.Database(
        "boundarymesh-database",
        {
            project: foundation.gcpProjectId,
            name: "boundarymesh",
            locationId: foundation.region,
            type: "FIRESTORE_NATIVE",
            databaseEdition: "STANDARD",
            appEngineIntegrationMode: "DISABLED",
            concurrencyMode: "PESSIMISTIC",
            deleteProtectionState: "DELETE_PROTECTION_ENABLED",
        },
        { dependsOn: [firestoreApi] },
    );

    return { firestoreApi, database };
}
