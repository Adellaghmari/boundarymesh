package dev.adellaghmari.boundarymesh.application.microsoft;

import dev.adellaghmari.boundarymesh.application.audit.AuditAppender;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingCommand;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingDeliveryService;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingDeliveryService.CrossingDeliveryResult;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.audit.AuditEventType;
import dev.adellaghmari.boundarymesh.domain.identity.AuthenticationMethod;
import dev.adellaghmari.boundarymesh.domain.identity.BoundaryPrincipal;
import dev.adellaghmari.boundarymesh.domain.identity.Issuer;
import dev.adellaghmari.boundarymesh.domain.identity.PrincipalId;
import dev.adellaghmari.boundarymesh.domain.identity.PrincipalType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MicrosoftCalendarCrossingService {

    public static final String INTEGRATION_ID = CalendarEventNormalizer.CONTRACT_ID;

    private final GraphCalendarPort calendar;
    private final CalendarEventNormalizer normalizer;
    private final CrossingDeliveryService delivery;
    private final AuditAppender audit;

    public MicrosoftCalendarCrossingService(
            GraphCalendarPort calendar,
            CalendarEventNormalizer normalizer,
            CrossingDeliveryService delivery,
            AuditAppender audit) {
        this.calendar = Objects.requireNonNull(calendar);
        this.normalizer = Objects.requireNonNull(normalizer);
        this.delivery = Objects.requireNonNull(delivery);
        this.audit = Objects.requireNonNull(audit);
    }

    public Result pull(BoundaryPrincipal principal, boolean live) {
        return pull(principal, live, calendar);
    }

    public Result pull(BoundaryPrincipal principal, boolean live, GraphCalendarPort port) {
        List<ProviderCalendarEvent> events;
        try {
            events = port.readUpcoming(5);
        } catch (GraphProviderException ex) {
            audit.append(
                    AuditChainId.ofIntegration(INTEGRATION_ID),
                    AuditEventType.CONNECTOR_READ,
                    Map.of(
                            "provider",
                            "MICROSOFT_GRAPH",
                            "status",
                            Integer.toString(ex.status()),
                            "retryable",
                            Boolean.toString(ex.retryable())));
            return new Result(false, live, List.of(), ex.status(), ex.getMessage());
        }
        audit.append(
                AuditChainId.ofIntegration(INTEGRATION_ID),
                AuditEventType.CONNECTOR_READ,
                Map.of(
                        "provider",
                        "MICROSOFT_GRAPH",
                        "count",
                        Integer.toString(events.size()),
                        "live",
                        Boolean.toString(live)));
        List<CrossingDeliveryResult> crossings = new ArrayList<>();
        for (ProviderCalendarEvent event : events) {
            String payload = normalizer.json(event);
            CrossingCommand command = new CrossingCommand(
                    principal,
                    ControlResult.pass(ControlKind.IDENTITY),
                    ControlResult.notApplicable(ControlKind.AUTHENTICITY),
                    ControlResult.notApplicable(ControlKind.FRESHNESS),
                    ControlResult.notApplicable(ControlKind.REPLAY),
                    INTEGRATION_ID,
                    CalendarEventNormalizer.CONTRACT_VERSION,
                    payload,
                    CalendarEventNormalizer.DESTINATION,
                    false,
                    null,
                    "active",
                    "valid",
                    "closed");
            crossings.add(delivery.evaluateWithoutDelivery(command, principal));
        }
        return new Result(true, live, List.copyOf(crossings), 200, null);
    }

    public static BoundaryPrincipal harnessPrincipal() {
        return new BoundaryPrincipal(
                PrincipalId.of("entra-harness-operator"),
                PrincipalType.USER,
                Issuer.of("https://login.microsoftonline.com/harness/v2.0"),
                AuthenticationMethod.OIDC,
                Set.of("Calendars.Read", "boundary.evaluate"),
                Set.of("OPERATOR"),
                "local-jca-p256");
    }

    public record Result(
            boolean connectorOk,
            boolean live,
            List<CrossingDeliveryResult> crossings,
            int providerStatus,
            String providerError) {}
}
