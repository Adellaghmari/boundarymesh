package dev.adellaghmari.boundarymesh.application.credential;

import dev.adellaghmari.boundarymesh.domain.credential.ConnectorIdentity;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialId;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialRecord;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialReference;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialStatus;
import dev.adellaghmari.boundarymesh.domain.credential.CredentialVersion;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCredentialCatalog implements CredentialCatalog {

    private final ConcurrentHashMap<String, CredentialRecord> records = new ConcurrentHashMap<>();

    public static InMemoryCredentialCatalog veloraDefaults(Instant origin) {
        InMemoryCredentialCatalog catalog = new InMemoryCredentialCatalog();
        catalog.put(record(
                "covecrm-api", "1", CredentialStatus.RETIRED, "covecrm-connector", "COVECRM", origin.minus(Duration.ofMinutes(41))));
        catalog.put(record(
                "covecrm-api", "2", CredentialStatus.ACTIVE, "covecrm-connector", "COVECRM", origin.minus(Duration.ofMinutes(8))));
        catalog.put(record(
                "lumenway-api", "1", CredentialStatus.ACTIVE, "lumenway-connector", "LUMENWAY", origin.minus(Duration.ofMinutes(19))));
        catalog.put(record(
                "rivertide-api", "1", CredentialStatus.ACTIVE, "rivertide-connector", "RIVERTIDE", origin.minus(Duration.ofMinutes(2))));
        catalog.put(record(
                "m365-graph", "1", CredentialStatus.ACTIVE, "m365-calendar-connector", "M365_CALENDAR", origin.minus(Duration.ofMinutes(5))));
        return catalog;
    }

    /** Production demo references aligned with the synthetic Secret Manager version-1 payloads. */
    public static InMemoryCredentialCatalog productionSyntheticDefaults(Instant rotatedAt) {
        InMemoryCredentialCatalog catalog = new InMemoryCredentialCatalog();
        catalog.put(record("covecrm-api", "1", CredentialStatus.ACTIVE, "covecrm-connector", "COVECRM", rotatedAt));
        catalog.put(record("lumenway-api", "1", CredentialStatus.ACTIVE, "lumenway-connector", "LUMENWAY", rotatedAt));
        catalog.put(
                record("rivertide-api", "1", CredentialStatus.ACTIVE, "rivertide-connector", "RIVERTIDE", rotatedAt));
        return catalog;
    }

    public void put(CredentialRecord record) {
        records.put(
                key(
                        record.reference().id().value(),
                        record.reference().version().value()),
                record);
    }

    public void rotate(String credentialId, String newVersion, Instant rotatedAt) {
        records.values().stream()
                .filter(item -> item.reference().id().value().equals(credentialId)
                        && item.reference().status() == CredentialStatus.ACTIVE)
                .toList()
                .forEach(active -> put(new CredentialRecord(
                        new CredentialReference(
                                active.reference().id(), active.reference().version(), CredentialStatus.RETIRED),
                        active.connector(),
                        active.destination(),
                        rotatedAt)));
        CredentialRecord seed = records.values().stream()
                .filter(item -> item.reference().id().value().equals(credentialId))
                .findFirst()
                .orElseThrow();
        put(record(
                credentialId,
                newVersion,
                CredentialStatus.ACTIVE,
                seed.connector().value(),
                seed.destination(),
                rotatedAt));
    }

    @Override
    public Optional<CredentialRecord> activeForDestination(String destination) {
        String dest = destination == null ? "" : destination.trim().toUpperCase(Locale.ROOT);
        return records.values().stream()
                .filter(item -> item.destination().equals(dest))
                .filter(item -> item.reference().status() == CredentialStatus.ACTIVE)
                .max(Comparator.comparing(
                        item -> Integer.parseInt(item.reference().version().value())));
    }

    @Override
    public Optional<CredentialRecord> find(String credentialId, String version) {
        return Optional.ofNullable(records.get(key(credentialId, version)));
    }

    @Override
    public List<CredentialRecord> list() {
        return records.values().stream()
                .sorted(Comparator.comparing((CredentialRecord item) -> item.destination())
                        .thenComparing(item -> item.reference().version().value()))
                .toList();
    }

    private static CredentialRecord record(
            String id,
            String version,
            CredentialStatus status,
            String connector,
            String destination,
            Instant rotatedAt) {
        return new CredentialRecord(
                new CredentialReference(new CredentialId(id), new CredentialVersion(version), status),
                new ConnectorIdentity(connector),
                destination,
                rotatedAt);
    }

    private static String key(String id, String version) {
        return id + ":" + version;
    }
}
