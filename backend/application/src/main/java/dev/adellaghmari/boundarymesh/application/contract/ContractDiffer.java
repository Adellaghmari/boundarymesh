package dev.adellaghmari.boundarymesh.application.contract;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Bounded structural comparison. Output is CONTRACT DIFFERENCE / POTENTIAL BREAKING CHANGE, not
 * mathematical compatibility certainty.
 */
public final class ContractDiffer {

    public List<String> compare(JsonNode left, JsonNode right) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        List<String> differences = new ArrayList<>();
        Set<String> requiredLeft = textSet(left.get("required"));
        Set<String> requiredRight = textSet(right.get("required"));
        for (String name : requiredRight) {
            if (!requiredLeft.contains(name)) {
                differences.add("POTENTIAL BREAKING CHANGE: required property added (" + name + ")");
            }
        }
        for (String name : requiredLeft) {
            if (!requiredRight.contains(name)) {
                differences.add("CONTRACT DIFFERENCE: required property removed (" + name + ")");
            }
        }
        JsonNode leftProps = left.get("properties");
        JsonNode rightProps = right.get("properties");
        if (leftProps != null && rightProps != null && leftProps.isObject() && rightProps.isObject()) {
            for (Map.Entry<String, JsonNode> entry : rightProps.properties()) {
                JsonNode previous = leftProps.get(entry.getKey());
                if (previous == null) {
                    differences.add("CONTRACT DIFFERENCE: property added (" + entry.getKey() + ")");
                    continue;
                }
                String leftType = typeOf(previous);
                String rightType = typeOf(entry.getValue());
                if (!leftType.isBlank() && !rightType.isBlank() && !leftType.equals(rightType)) {
                    differences.add("POTENTIAL BREAKING CHANGE: type changed (" + entry.getKey() + ")");
                }
            }
            for (Map.Entry<String, JsonNode> entry : leftProps.properties()) {
                if (rightProps.get(entry.getKey()) == null) {
                    differences.add("CONTRACT DIFFERENCE: property removed (" + entry.getKey() + ")");
                }
            }
        }
        return List.copyOf(differences);
    }

    private static Set<String> textSet(JsonNode node) {
        Set<String> values = new LinkedHashSet<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        node.forEach(item -> {
            if (item.isString()) {
                values.add(item.stringValue());
            }
        });
        return values;
    }

    private static String typeOf(JsonNode node) {
        JsonNode type = node.get("type");
        return type == null || !type.isString() ? "" : type.stringValue();
    }
}
