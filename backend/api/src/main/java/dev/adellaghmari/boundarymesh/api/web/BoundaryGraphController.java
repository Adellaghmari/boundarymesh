package dev.adellaghmari.boundarymesh.api.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.adellaghmari.boundarymesh.application.graph.BoundaryGraphAssembler;
import dev.adellaghmari.boundarymesh.domain.graph.BoundaryEdge;
import dev.adellaghmari.boundarymesh.domain.graph.BoundaryGraph;
import dev.adellaghmari.boundarymesh.domain.graph.SystemNode;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/boundary-graph", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoundaryGraphController {

    private final BoundaryGraphAssembler assembler;

    public BoundaryGraphController(BoundaryGraphAssembler assembler) {
        this.assembler = assembler;
    }

    @GetMapping
    public GraphView graph() {
        return GraphView.from(assembler.assemble());
    }

    public record GraphView(String banner, List<NodeView> nodes, List<EdgeView> edges) {
        static GraphView from(BoundaryGraph graph) {
            return new GraphView(
                    graph.banner(),
                    graph.nodes().stream().map(NodeView::from).toList(),
                    graph.edges().stream().map(EdgeView::from).toList());
        }
    }

    public record NodeView(String id, String label, String kind, boolean synthetic) {
        static NodeView from(SystemNode node) {
            return new NodeView(node.id(), node.label(), node.kind().name(), node.synthetic());
        }
    }

    public record EdgeView(
            String id,
            String source,
            String destination,
            @JsonProperty("integration_id") String integrationId,
            @JsonProperty("identity_mechanism") String identityMechanism,
            @JsonProperty("contract_reference") String contractReference,
            @JsonProperty("egress_policy") String egressPolicy,
            @JsonProperty("credential_reference") String credentialReference,
            @JsonProperty("consent_state") String consentState,
            @JsonProperty("connector_route") String connectorRoute,
            String status,
            boolean configured,
            boolean observed,
            @JsonProperty("recent_outcome") String recentOutcome,
            @JsonProperty("recent_delivery") String recentDelivery,
            @JsonProperty("quarantine_count") int quarantineCount,
            @JsonProperty("audit_chain_id") String auditChainId,
            @JsonProperty("attestation_id") String attestationId) {

        static EdgeView from(BoundaryEdge edge) {
            return new EdgeView(
                    edge.id(),
                    edge.source(),
                    edge.destination(),
                    edge.integration().integrationId(),
                    edge.identityMechanism(),
                    edge.contractReference(),
                    edge.egressPolicy(),
                    edge.credentialReference(),
                    edge.consentState(),
                    edge.connectorRoute(),
                    edge.status().name(),
                    edge.configured(),
                    edge.observed(),
                    edge.recentOutcome(),
                    edge.recentDelivery(),
                    edge.quarantineCount(),
                    edge.auditChainId(),
                    edge.attestationId());
        }
    }
}
