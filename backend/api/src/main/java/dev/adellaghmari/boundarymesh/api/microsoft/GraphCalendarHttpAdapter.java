package dev.adellaghmari.boundarymesh.api.microsoft;

import dev.adellaghmari.boundarymesh.application.microsoft.GraphCalendarPort;
import dev.adellaghmari.boundarymesh.application.microsoft.GraphProviderException;
import dev.adellaghmari.boundarymesh.application.microsoft.ProviderCalendarEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Graph-shaped HTTP client used for WireMock provider tests. Not used by the live SDK path. */
public final class GraphCalendarHttpAdapter implements GraphCalendarPort {

    private final String baseUrl;
    private final Supplier<String> accessToken;
    private final HttpClient http;
    private final JsonMapper mapper = JsonMapper.shared();

    public GraphCalendarHttpAdapter(String baseUrl, Supplier<String> accessToken) {
        this.baseUrl = baseUrl;
        this.accessToken = accessToken;
        this.http =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @Override
    public List<ProviderCalendarEvent> readUpcoming(int limit) {
        String token = accessToken.get();
        OffsetDateTime start = OffsetDateTime.parse("2026-09-10T12:00:00Z");
        String uri = baseUrl + "/v1.0/me/calendarView?startDateTime="
                + enc(start.toString())
                + "&endDateTime="
                + enc(start.plusDays(7).toString())
                + "&$top="
                + Math.min(limit, 5);
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(3))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 401 || status == 403 || status == 429 || status >= 500) {
                String body = response.body();
                String code = GraphErrorSanitizer.code(body);
                boolean emptyBody = body == null || body.isBlank();
                boolean www = response.headers().firstValue("WWW-Authenticate").isPresent();
                throw new GraphProviderException(
                        status,
                        "graph provider error",
                        response.headers().firstValue("Retry-After").orElse(null),
                        code,
                        GraphErrorSanitizer.category(status, code, emptyBody),
                        www);
            }
            if (status != 200) {
                throw new GraphProviderException(status, "graph provider error", null);
            }
            return parse(response.body());
        } catch (GraphProviderException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new GraphProviderException(0, "graph provider timeout or transport failure", null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GraphProviderException(0, "graph provider interrupted", null);
        }
    }

    private List<ProviderCalendarEvent> parse(String body) {
        JsonNode root = mapper.readTree(body);
        JsonNode value = root.get("value");
        if (value == null || !value.isArray()) {
            throw new GraphProviderException(502, "malformed provider response", null);
        }
        List<ProviderCalendarEvent> events = new ArrayList<>();
        for (JsonNode node : value) {
            List<String> attendees = new ArrayList<>();
            JsonNode attendeeNode = node.get("attendees");
            if (attendeeNode != null && attendeeNode.isArray()) {
                for (JsonNode attendee : attendeeNode) {
                    String address = nested(attendee, "emailAddress", "address");
                    if (!address.isBlank()) {
                        attendees.add(address);
                    }
                }
            }
            events.add(new ProviderCalendarEvent(
                    text(node, "id"),
                    text(node, "subject"),
                    nested(node, "start", "dateTime"),
                    nested(node, "end", "dateTime"),
                    text(node, "showAs"),
                    attendees,
                    nested(node, "onlineMeeting", "joinUrl"),
                    nested(node, "body", "content")));
        }
        return List.copyOf(events);
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return "";
        }
        JsonNode value = node.get(field);
        return value == null || !value.isString() ? "" : value.stringValue();
    }

    private static String nested(JsonNode node, String first, String second) {
        if (node == null) {
            return "";
        }
        JsonNode child = node.get(first);
        return text(child, second);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
