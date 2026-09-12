package dev.adellaghmari.boundarymesh.gcp;

/**
 * Intended composition modes. Phase 15C does not activate a production GCP adapter in any mode.
 * Local and test remain the default application profiles.
 */
public enum GcpRuntimeMode {
    LOCAL,
    TEST,
    GCP_EMULATOR,
    PROD
}
