package dev.adellaghmari.boundarymesh.application.microsoft;

import dev.adellaghmari.boundarymesh.application.canon.Sha256;
import java.util.List;
import java.util.Objects;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class CalendarEventNormalizer {

    public static final String CONTRACT_ID = "m365-calendar";
    public static final String CONTRACT_VERSION = "v1";
    public static final String DESTINATION = "M365_CALENDAR";

    private final JsonMapper mapper;

    public CalendarEventNormalizer() {
        this(JsonMapper.shared());
    }

    public CalendarEventNormalizer(JsonMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    public String json(ProviderCalendarEvent event) {
        Objects.requireNonNull(event, "event");
        ObjectNode node = mapper.createObjectNode();
        node.put("event_reference", Sha256.hexUtf8(event.providerId() == null ? "" : event.providerId()));
        node.put("start_time", nullToEmpty(event.startTime()));
        node.put("end_time", nullToEmpty(event.endTime()));
        node.put("subject", nullToEmpty(event.subject()));
        node.put("availability", nullToEmpty(event.availability()));
        ArrayNode attendees = node.putArray("attendees");
        List<String> values = event.attendees() == null ? List.of() : event.attendees();
        values.forEach(attendees::add);
        node.put("online_meeting_url", nullToEmpty(event.onlineMeetingUrl()));
        node.put("body", nullToEmpty(event.body()));
        return mapper.writeValueAsString(node);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
