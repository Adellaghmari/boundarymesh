package dev.adellaghmari.boundarymesh.gcp;

/**
 * Intended composition modes. LOCAL and TEST keep in-process adapters. GCP_EMULATOR may select
 * Firestore emulator adapters. Secret Manager has no emulator. PROD may select Secret Manager and
 * Firestore production adapters when a later phase wires this module under an explicit cloud
 * profile. Neither mode is the default local runtime.
 */
public enum GcpRuntimeMode {
    LOCAL,
    TEST,
    GCP_EMULATOR,
    PROD
}
