package dev.adellaghmari.boundarymesh.application.graph;

import static org.assertj.core.api.Assertions.assertThat;

import dev.adellaghmari.boundarymesh.application.quarantine.InMemoryQuarantineRepository;
import dev.adellaghmari.boundarymesh.application.quarantine.QuarantineOrchestrator;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import dev.adellaghmari.boundarymesh.domain.graph.BoundaryEdge;
import dev.adellaghmari.boundarymesh.domain.graph.BoundaryEdgeStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BoundaryGraphAssemblerTest {

    private ObservedCrossingStore observations;
    private InMemoryQuarantineRepository quarantine;
    private BoundaryGraphAssembler assembler;

    @BeforeEach
    void setup() {
        observations = new InMemoryObservedCrossingStore();
        quarantine = new InMemoryQuarantineRepository();
        assembler = new BoundaryGraphAssembler(observations, quarantine);
    }

    @Test
    void configuredEdgesExistWithoutTraffic() {
        var graph = assembler.assemble();
        assertThat(graph.nodes())
                .extracting(node -> node.id())
                .containsExactly("QUAYFORGE", "M365", "HELIX365", "BOUNDARYMESH", "COVECRM", "LUMENWAY", "RIVERTIDE");
        assertThat(graph.edges()).hasSize(5);
        assertThat(edge(graph.edges().getFirst()).status()).isEqualTo(BoundaryEdgeStatus.CONFIGURED);
        assertThat(edge(graph.edges().getFirst()).observed()).isFalse();
        assertThat(helix().status()).isEqualTo(BoundaryEdgeStatus.NO_RECENT_ACTIVITY);
        assertThat(graph.banner()).contains("LOCAL");
        assertThat(graph.toString()).doesNotContain("TEST-SECRET");
    }

    @Test
    void observedDeliveryMarksDestinationActive() {
        observations.record(observation("LUMENWAY", "ALLOW_WITH_TRANSFORMATION", "DELIVERED", false));
        assertThat(lumenway().status()).isEqualTo(BoundaryEdgeStatus.ACTIVE);
        assertThat(lumenway().observed()).isTrue();
        assertThat(lumenway().recentDelivery()).isEqualTo("DELIVERED");
        assertThat(covecrm().status()).isEqualTo(BoundaryEdgeStatus.CONFIGURED);
    }

    @Test
    void rejectMarksCoveCrmBlocked() {
        observations.record(observation("COVECRM", "REJECT", "NOT_ATTEMPTED", false));
        assertThat(covecrm().status()).isEqualTo(BoundaryEdgeStatus.BLOCKED);
        assertThat(covecrm().recentOutcome()).isEqualTo("REJECT");
    }

    @Test
    void quarantineObservationUpdatesCountAndStatus() {
        new QuarantineOrchestrator(quarantine, Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC))
                .open(
                        TransactionId.of("11111111-2222-3333-4444-555555555555"),
                        "quayforge-orders",
                        "QUAYFORGE",
                        "RIVERTIDE",
                        ReasonCode.CONTRACT_DRIFT_DETECTED,
                        "v1",
                        "rivertide",
                        "rivertide-api:1",
                        "CLOSED");
        observations.record(observation("RIVERTIDE", "QUARANTINE", "NOT_ATTEMPTED", true));
        assertThat(rivertide().status()).isEqualTo(BoundaryEdgeStatus.QUARANTINED);
        assertThat(rivertide().quarantineCount()).isEqualTo(1);
        assertThat(helix().status()).isEqualTo(BoundaryEdgeStatus.NO_RECENT_ACTIVITY);
    }

    private BoundaryEdge helix() {
        return assembler.assemble().edges().stream()
                .filter(edge -> "helix365-calendar".equals(edge.id()))
                .findFirst()
                .orElseThrow();
    }

    private BoundaryEdge covecrm() {
        return assembler.assemble().edges().stream()
                .filter(edge -> "COVECRM".equals(edge.destination()))
                .filter(edge -> !"helix365-calendar".equals(edge.id()))
                .filter(edge -> !"m365-calendar".equals(edge.id()))
                .findFirst()
                .orElseThrow();
    }

    private BoundaryEdge lumenway() {
        return assembler.assemble().edges().stream()
                .filter(edge -> "LUMENWAY".equals(edge.destination()))
                .findFirst()
                .orElseThrow();
    }

    private BoundaryEdge rivertide() {
        return assembler.assemble().edges().stream()
                .filter(edge -> "RIVERTIDE".equals(edge.destination()))
                .findFirst()
                .orElseThrow();
    }

    private static BoundaryEdge edge(BoundaryEdge value) {
        return value;
    }

    private static ObservedCrossingStore.Observation observation(
            String destination, String outcome, String delivery, boolean quarantined) {
        return new ObservedCrossingStore.Observation(
                "quayforge-orders",
                "QUAYFORGE",
                destination,
                outcome,
                delivery,
                "quayforge-orders",
                "att-1",
                quarantined,
                Instant.parse("2026-09-10T12:00:00Z"));
    }
}
