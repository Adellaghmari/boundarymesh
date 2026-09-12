package dev.adellaghmari.boundarymesh.gcp;

/**
 * Keeps default {@code mvnw verify} independent of ADC, Docker, and a running emulator.
 * Opt-in emulator suites use the JUnit tag {@code gcp-emulator} and this environment flag.
 */
public final class GcpEmulatorGuard {

    public static final String EMULATOR_FLAG = "BOUNDARYMESH_GCP_EMULATOR";
    public static final String EMULATOR_TEST_TAG = "gcp-emulator";

    private GcpEmulatorGuard() {}

    public static boolean cloudCredentialsRequiredForDefaultTests() {
        return false;
    }

    public static boolean emulatorIntegrationEnabled() {
        return Boolean.parseBoolean(System.getenv(EMULATOR_FLAG));
    }
}
