package dev.adellaghmari.boundarymesh.gcp.firestore;

import java.util.Objects;
import java.util.Optional;

/**
 * Central Firestore configuration. Database ID defaults to {@code boundarymesh}. Emulator host
 * selects the local emulator and disables ADC. Production construction is explicit and unused by
 * default tests.
 */
public record FirestoreSettings(String projectId, String databaseId, Optional<String> emulatorHost) {

    public static final String DEFAULT_DATABASE_ID = "boundarymesh";

    public FirestoreSettings {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(databaseId, "databaseId");
        Objects.requireNonNull(emulatorHost, "emulatorHost");
        if (projectId.isBlank() || databaseId.isBlank()) {
            throw new IllegalArgumentException("projectId and databaseId are required");
        }
        if ("(default)".equals(databaseId) || "default".equalsIgnoreCase(databaseId)) {
            throw new IllegalArgumentException("Named database 'boundarymesh' is required; (default) is forbidden");
        }
        emulatorHost = emulatorHost.map(String::trim).filter(host -> !host.isBlank());
    }

    public static FirestoreSettings emulator(String projectId, String emulatorHost) {
        return new FirestoreSettings(projectId, DEFAULT_DATABASE_ID, Optional.of(emulatorHost));
    }

    public static FirestoreSettings emulator(String projectId, String databaseId, String emulatorHost) {
        return new FirestoreSettings(projectId, databaseId, Optional.of(emulatorHost));
    }

    public static FirestoreSettings production(String projectId) {
        return new FirestoreSettings(projectId, DEFAULT_DATABASE_ID, Optional.empty());
    }

    public boolean usesEmulator() {
        return emulatorHost.isPresent();
    }

    public String databaseResourceName() {
        return "projects/" + projectId + "/databases/" + databaseId;
    }
}
