package dev.adellaghmari.boundarymesh.application.consent;

import dev.adellaghmari.boundarymesh.domain.consent.ConnectorOperation;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentGrant;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentId;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentStatus;
import dev.adellaghmari.boundarymesh.domain.consent.GrantKind;
import dev.adellaghmari.boundarymesh.domain.consent.Permission;
import dev.adellaghmari.boundarymesh.domain.credential.ConnectorIdentity;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryConsentCatalog implements ConsentCatalog {

    private final ConcurrentHashMap<String, ConsentGrant> grants = new ConcurrentHashMap<>();

    public static InMemoryConsentCatalog veloraDefaults() {
        InMemoryConsentCatalog catalog = new InMemoryConsentCatalog();
        catalog.put(grant(
                "covecrm-activity",
                "covecrm-connector",
                "COVECRM",
                ConnectorOperation.ACTIVITY_WRITE,
                "activity.write"));
        catalog.put(grant(
                "lumenway-fulfillment",
                "lumenway-connector",
                "LUMENWAY",
                ConnectorOperation.FULFILLMENT_CREATE,
                "fulfillment.create"));
        catalog.put(grant(
                "rivertide-settlement",
                "rivertide-connector",
                "RIVERTIDE",
                ConnectorOperation.SETTLEMENT_WRITE,
                "settlement.write"));
        catalog.put(new ConsentGrant(
                new ConsentId("m365-calendar-read"),
                new ConnectorIdentity("m365-calendar-connector"),
                "M365_CALENDAR",
                ConnectorOperation.CALENDAR_READ,
                Set.of(new Permission("Calendars.Read")),
                ConsentStatus.GRANTED,
                GrantKind.DELEGATED,
                "entra-tenant"));
        return catalog;
    }

    public void put(ConsentGrant grant) {
        grants.put(key(grant.destination(), grant.operation()), grant);
    }

    public void revoke(String destination, ConnectorOperation operation) {
        find(destination, operation)
                .ifPresent(grant -> put(new ConsentGrant(
                        grant.id(),
                        grant.connector(),
                        grant.destination(),
                        grant.operation(),
                        grant.permissions(),
                        ConsentStatus.REVOKED,
                        grant.kind(),
                        grant.tenantRef())));
    }

    public void remove(String destination, ConnectorOperation operation) {
        grants.remove(key(destination, operation));
    }

    @Override
    public Optional<ConsentGrant> find(String destination, ConnectorOperation operation) {
        return Optional.ofNullable(grants.get(key(destination, operation)));
    }

    @Override
    public List<ConsentGrant> list() {
        return List.copyOf(grants.values());
    }

    private static ConsentGrant grant(
            String id, String connector, String destination, ConnectorOperation operation, String permission) {
        return new ConsentGrant(
                new ConsentId(id),
                new ConnectorIdentity(connector),
                destination,
                operation,
                Set.of(new Permission(permission)),
                ConsentStatus.GRANTED,
                GrantKind.APPLICATION,
                "synthetic-tenant");
    }

    private static String key(String destination, ConnectorOperation operation) {
        return destination.trim().toUpperCase(Locale.ROOT) + ":" + operation.value();
    }
}
