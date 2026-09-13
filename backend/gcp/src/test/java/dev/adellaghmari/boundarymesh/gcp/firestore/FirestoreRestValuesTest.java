package dev.adellaghmari.boundarymesh.gcp.firestore;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FirestoreRestValuesTest {

    @Test
    void roundTripsSyntheticDocumentFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("nonce_hash", "fake-replay-nonce-15fa");
        fields.put("sequence", 2L);
        fields.put("ok", true);
        fields.put("attributes", Map.of("step", "first"));
        fields.put("attempts", List.of(Map.of("note", "fake-note")));
        JsonObject encoded = FirestoreRestValues.fieldsOf(fields);
        Map<String, Object> decoded = FirestoreRestValues.fromFields(encoded);
        assertThat(decoded.get("nonce_hash")).isEqualTo("fake-replay-nonce-15fa");
        assertThat(decoded.get("sequence")).isEqualTo(2L);
        assertThat(decoded.get("ok")).isEqualTo(true);
        assertThat(decoded.get("attributes")).isEqualTo(Map.of("step", "first"));
        assertThat(String.valueOf(decoded.values())).doesNotContain("BEGIN PRIVATE", "Bearer ", "TEST-SECRET");
    }
}
