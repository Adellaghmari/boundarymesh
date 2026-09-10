package dev.adellaghmari.boundarymesh.api.egress;

import dev.adellaghmari.boundarymesh.application.egress.EgressPolicyRepository;
import dev.adellaghmari.boundarymesh.domain.egress.DenyMode;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicy;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyId;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyVersion;
import dev.adellaghmari.boundarymesh.domain.egress.FieldAction;
import dev.adellaghmari.boundarymesh.domain.egress.FieldPath;
import dev.adellaghmari.boundarymesh.domain.egress.FieldRule;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ResourceEgressPolicyRepository implements EgressPolicyRepository {

    private final Map<String, EgressPolicy> byDestination = new ConcurrentHashMap<>();

    public ResourceEgressPolicyRepository() {
        register(load("contracts/policies/rivertide.egress.v1.json"));
        register(load("contracts/policies/covecrm.egress.v1.json"));
        register(load("contracts/policies/lumenway.egress.v1.json"));
        register(load("contracts/policies/m365-calendar.egress.v1.json"));
    }

    @Override
    public Optional<EgressPolicy> findByDestination(String destination) {
        return Optional.ofNullable(byDestination.get(destination));
    }

    private void register(EgressPolicy policy) {
        byDestination.put(policy.destination(), policy);
    }

    static EgressPolicy load(String classpath) {
        ClassPathResource resource = new ClassPathResource(classpath);
        try (InputStream input = resource.getInputStream()) {
            JsonNode node = JsonMapper.shared().readTree(input);
            return parse(node);
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Invalid egress policy " + classpath, ex);
        }
    }

    public static EgressPolicy parse(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("policy must be an object");
        }
        if (missing(node, "id") || missing(node, "version") || missing(node, "destination")) {
            throw new IllegalArgumentException("policy requires id, version, and destination");
        }
        List<FieldRule> rules = new ArrayList<>();
        JsonNode rulesNode = node.get("rules");
        if (rulesNode == null || !rulesNode.isArray() || rulesNode.size() == 0) {
            throw new IllegalArgumentException("policy requires rules");
        }
        for (JsonNode rule : rulesNode) {
            FieldAction action = FieldAction.valueOf(text(rule, "action"));
            DenyMode denyMode = action == FieldAction.DENY ? DenyMode.valueOf(text(rule, "deny_mode")) : null;
            rules.add(new FieldRule(text(rule, "rule_id"), FieldPath.parse(text(rule, "path")), action, denyMode));
        }
        JsonNode implicit = node.get("implicit_deny");
        boolean implicitDeny = implicit == null || implicit.isNull() || implicit.booleanValue();
        return new EgressPolicy(
                new EgressPolicyId(text(node, "id")),
                new EgressPolicyVersion(text(node, "version")),
                text(node, "destination"),
                implicitDeny,
                rules);
    }

    private static boolean missing(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.stringValue().isBlank()) {
            throw new IllegalArgumentException("missing " + field);
        }
        return value.stringValue();
    }
}
