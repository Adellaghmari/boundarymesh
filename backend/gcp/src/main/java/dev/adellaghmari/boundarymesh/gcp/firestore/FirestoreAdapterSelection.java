package dev.adellaghmari.boundarymesh.gcp.firestore;

import dev.adellaghmari.boundarymesh.gcp.GcpRuntimeMode;

/**
 * Composition helper. LOCAL and TEST keep in-memory adapters. GCP_EMULATOR may select emulator
 * Firestore adapters. PROD may select production adapters later. This module is not Spring-wired
 * into API or worker in Phase 15F-A.
 */
public final class FirestoreAdapterSelection {

    private FirestoreAdapterSelection() {}

    public static boolean firestorePersistenceSelected(GcpRuntimeMode mode) {
        return mode == GcpRuntimeMode.GCP_EMULATOR || mode == GcpRuntimeMode.PROD;
    }

    public static boolean requiresAdc(GcpRuntimeMode mode) {
        return mode == GcpRuntimeMode.PROD;
    }
}
