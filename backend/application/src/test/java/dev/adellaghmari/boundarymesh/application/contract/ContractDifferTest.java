package dev.adellaghmari.boundarymesh.application.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ContractDifferTest {

    @Test
    void labelsRequiredAndTypeChangesAsPotentialBreaking() {
        JsonMapper mapper = JsonMapper.shared();
        var left = mapper.readTree(
                """
                {"required":["event_id"],"properties":{"event_id":{"type":"string"},"amount":{"type":"number"}}}
                """);
        var right = mapper.readTree(
                """
                {"required":["event_id","order_status"],"properties":{"event_id":{"type":"string"},"amount":{"type":"object"},"order_status":{"type":"string"}}}
                """);
        var differences = new ContractDiffer().compare(left, right);
        assertThat(differences)
                .anyMatch(item -> item.contains("POTENTIAL BREAKING CHANGE") && item.contains("order_status"))
                .anyMatch(item -> item.contains("POTENTIAL BREAKING CHANGE") && item.contains("amount"));
    }
}
