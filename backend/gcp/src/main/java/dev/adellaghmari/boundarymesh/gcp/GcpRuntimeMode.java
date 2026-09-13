package dev.adellaghmari.boundarymesh.gcp;

/**
 * Intended composition modes. LOCAL and TEST keep in-process adapters. GCP_EMULATOR is reserved for
 * Firestore/Pub/Sub later; Secret Manager has no emulator. PROD may select Secret Manager adapters
 * when a later phase wires this module under an explicit cloud profile.
 */
public enum GcpRuntimeMode {
    LOCAL,
    TEST,
    GCP_EMULATOR,
    PROD
}
