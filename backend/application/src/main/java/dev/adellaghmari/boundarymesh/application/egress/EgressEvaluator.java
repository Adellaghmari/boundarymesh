package dev.adellaghmari.boundarymesh.application.egress;

import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.egress.DenyMode;
import dev.adellaghmari.boundarymesh.domain.egress.EgressEvaluation;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicy;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyId;
import dev.adellaghmari.boundarymesh.domain.egress.EgressPolicyVersion;
import dev.adellaghmari.boundarymesh.domain.egress.FieldAction;
import dev.adellaghmari.boundarymesh.domain.egress.FieldDecision;
import dev.adellaghmari.boundarymesh.domain.egress.FieldPath;
import dev.adellaghmari.boundarymesh.domain.egress.FieldRule;
import dev.adellaghmari.boundarymesh.domain.egress.RedactionSummary;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public final class EgressEvaluator {

    static final String REDACTED = "[REDACTED]";

    private final EgressPolicyRepository repository;
    private final JsonMapper mapper;

    public EgressEvaluator(EgressPolicyRepository repository) {
        this(repository, JsonMapper.shared());
    }

    public EgressEvaluator(EgressPolicyRepository repository, JsonMapper mapper) {
        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public EgressEvaluation evaluate(String destination, String sourceJson) {
        Optional<EgressPolicy> found = repository.findByDestination(destination);
        if (found.isEmpty()) {
            return new EgressEvaluation(
                    new EgressPolicyId("unavailable"),
                    new EgressPolicyVersion("none"),
                    destination,
                    ControlResult.unavailable(
                            ControlKind.EGRESS,
                            ReasonCode.EGRESS_POLICY_UNAVAILABLE,
                            "No egress policy is registered for this destination."),
                    List.of(),
                    new RedactionSummary(List.of(), List.of(), List.of(), List.of(), 0),
                    null);
        }
        EgressPolicy policy = found.get();
        JsonNode source = mapper.readTree(sourceJson);
        if (!source.isObject()) {
            return new EgressEvaluation(
                    policy.id(),
                    policy.version(),
                    policy.destination(),
                    ControlResult.fail(
                            ControlKind.EGRESS, ReasonCode.EGRESS_POLICY_INVALID, "Egress expects a JSON object."),
                    List.of(),
                    new RedactionSummary(List.of(), List.of(), List.of(), List.of(), 0),
                    null);
        }
        List<FieldDecision> decisions = new ArrayList<>();
        List<String> allowed = new ArrayList<>();
        List<String> redacted = new ArrayList<>();
        List<String> denied = new ArrayList<>();
        List<String> implicit = new ArrayList<>();
        for (FieldRule rule : policy.rules()) {
            JsonNode present = source.at(rule.path().pointer());
            if (present.isMissingNode()) {
                continue;
            }
            if (rule.action() == FieldAction.DENY && rule.denyMode() == DenyMode.REJECT) {
                denied.add(rule.path().pointer());
                decisions.add(new FieldDecision(rule.path(), FieldAction.DENY, rule.ruleId(), "Critical deny."));
                return new EgressEvaluation(
                        policy.id(),
                        policy.version(),
                        policy.destination(),
                        ControlResult.fail(
                                ControlKind.EGRESS,
                                ReasonCode.EGRESS_FIELD_DENIED,
                                "A forbidden critical field was present."),
                        decisions,
                        new RedactionSummary(allowed, redacted, denied, implicit, 0),
                        null);
            }
        }
        ObjectNode outbound = mapper.createObjectNode();
        Set<String> governed = new LinkedHashSet<>();
        for (FieldRule rule : policy.rules()) {
            JsonNode present = source.at(rule.path().pointer());
            if (present.isMissingNode()) {
                continue;
            }
            governed.add(rule.path().pointer());
            switch (rule.action()) {
                case ALLOW -> {
                    copy(outbound, rule.path().pointer(), present);
                    allowed.add(rule.path().pointer());
                    decisions.add(new FieldDecision(rule.path(), FieldAction.ALLOW, rule.ruleId(), "Copied."));
                }
                case REDACT -> {
                    copy(
                            outbound,
                            rule.path().pointer(),
                            mapper.getNodeFactory().stringNode(REDACTED));
                    redacted.add(rule.path().pointer());
                    decisions.add(new FieldDecision(rule.path(), FieldAction.REDACT, rule.ruleId(), "Redacted."));
                }
                case DENY -> {
                    denied.add(rule.path().pointer());
                    decisions.add(new FieldDecision(rule.path(), FieldAction.DENY, rule.ruleId(), "Stripped."));
                }
            }
        }
        if (policy.implicitDeny()) {
            for (String pointer : pointers(source, "")) {
                if (pointer.isEmpty() || covered(governed, pointer)) {
                    continue;
                }
                implicit.add(pointer);
                decisions.add(new FieldDecision(
                        FieldPath.parse(pointer), FieldAction.DENY, "IMPLICIT-DENY", "Unlisted field omitted."));
            }
        }
        boolean transformed = !redacted.isEmpty() || !denied.isEmpty() || !implicit.isEmpty();
        ControlResult control = transformed
                ? ControlResult.pass(
                        ControlKind.EGRESS,
                        ReasonCode.EGRESS_REDACTION_REQUIRED,
                        "Destination policy transformed the outbound payload.")
                : new ControlResult(
                        ControlKind.EGRESS,
                        dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                        List.of(),
                        "All present fields were explicitly allowed.",
                        policy.id().value() + ":" + policy.version());
        return new EgressEvaluation(
                policy.id(),
                policy.version(),
                policy.destination(),
                control,
                decisions,
                new RedactionSummary(
                        allowed, redacted, denied, implicit, redacted.size() + implicit.size() + denied.size()),
                outbound.toString());
    }

    private static boolean covered(Set<String> governed, String pointer) {
        if (governed.contains(pointer)) {
            return true;
        }
        String current = pointer;
        while (true) {
            int slash = current.lastIndexOf('/');
            if (slash <= 0) {
                return false;
            }
            current = current.substring(0, slash);
            if (governed.contains(current)) {
                return true;
            }
        }
    }

    private static void copy(ObjectNode root, String pointer, JsonNode value) {
        if (pointer.isEmpty() || "/".equals(pointer)) {
            return;
        }
        String[] tokens = pointer.substring(1).split("/");
        ObjectNode cursor = root;
        for (int i = 0; i < tokens.length - 1; i++) {
            String token = unescape(tokens[i]);
            JsonNode child = cursor.get(token);
            if (child == null || !child.isObject()) {
                child = cursor.putObject(token);
            }
            cursor = (ObjectNode) child;
        }
        cursor.set(unescape(tokens[tokens.length - 1]), value);
    }

    private static List<String> pointers(JsonNode node, String prefix) {
        List<String> paths = new ArrayList<>();
        if (!prefix.isEmpty()) {
            paths.add(prefix);
        }
        if (node.isObject()) {
            node.properties()
                    .forEach(entry -> paths.addAll(pointers(entry.getValue(), prefix + "/" + escape(entry.getKey()))));
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                paths.addAll(pointers(node.get(i), prefix + "/" + i));
            }
        }
        return paths;
    }

    private static String escape(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }

    private static String unescape(String token) {
        return token.replace("~1", "/").replace("~0", "~");
    }
}
