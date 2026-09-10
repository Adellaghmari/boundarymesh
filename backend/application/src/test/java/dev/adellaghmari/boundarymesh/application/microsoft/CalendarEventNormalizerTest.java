package dev.adellaghmari.boundarymesh.application.microsoft;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CalendarEventNormalizerTest {

    @Test
    void hashesProviderIdAndKeepsSafeFields() {
        String json = new CalendarEventNormalizer()
                .json(new ProviderCalendarEvent(
                        "raw-graph-id",
                        "Secret subject",
                        "2026-09-10T13:00:00Z",
                        "2026-09-10T14:00:00Z",
                        "busy",
                        List.of("ada.lovelace@example.test"),
                        "https://teams.microsoft.com/l/meetup-join/x",
                        "private notes"));
        var node = JsonMapper.shared().readTree(json);
        assertThat(node.get("event_reference").stringValue()).hasSize(64);
        assertThat(node.get("event_reference").stringValue()).doesNotContain("raw-graph-id");
        assertThat(node.get("subject").stringValue()).isEqualTo("Secret subject");
        assertThat(json).doesNotContain("Bearer ");
    }
}
