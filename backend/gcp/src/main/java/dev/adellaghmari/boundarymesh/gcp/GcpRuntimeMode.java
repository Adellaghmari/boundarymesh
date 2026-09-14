package dev.adellaghmari.boundarymesh.gcp;

/**
 * Intended composition modes. LOCAL and TEST keep in-process adapters. GCP_EMULATOR may select
 * Firestore emulator adapters. Secret Manager has no emulator. PROD may select Secret Manager and
 * Firestore, Pub/Sub, and Cloud KMS production adapters when a later phase wires this module under
 * an explicit cloud profile. Neither mode is the default local runtime. Cloud KMS is not selected
 * for LOCAL, TEST, or GCP_EMULATOR.
 */
public enum GcpRuntimeMode {
    LOCAL,
    TEST,
    GCP_EMULATOR,
    PROD
}
