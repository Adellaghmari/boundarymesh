package dev.adellaghmari.boundarymesh.api.microsoft;

import com.microsoft.graph.models.Attendee;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.authentication.AccessTokenProvider;
import com.microsoft.kiota.authentication.AllowedHostsValidator;
import com.microsoft.kiota.authentication.BaseBearerTokenAuthenticationProvider;
import dev.adellaghmari.boundarymesh.application.microsoft.GraphCalendarPort;
import dev.adellaghmari.boundarymesh.application.microsoft.GraphProviderException;
import dev.adellaghmari.boundarymesh.application.microsoft.ProviderCalendarEvent;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Live Microsoft Graph calendar adapter. Uses the official Graph Java SDK. Domain types never
 * leak SDK classes.
 */
public final class MicrosoftGraphCalendarAdapter implements GraphCalendarPort {

    private final Supplier<String> accessToken;

    public MicrosoftGraphCalendarAdapter(Supplier<String> accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public List<ProviderCalendarEvent> readUpcoming(int limit) {
        String token = accessToken.get();
        if (token == null || token.isBlank()) {
            throw new GraphProviderException(401, "graph access token missing", null);
        }
        try {
            GraphServiceClient client =
                    new GraphServiceClient(new BaseBearerTokenAuthenticationProvider(new StaticTokenProvider(token)));
            OffsetDateTime start = OffsetDateTime.now();
            int top = Math.min(Math.max(limit, 1), 5);
            var page = client.me().calendarView().get(request -> {
                request.queryParameters.startDateTime = start.toString();
                request.queryParameters.endDateTime = start.plusDays(7).toString();
                request.queryParameters.top = top;
                request.queryParameters.select = new String[] {"id", "start", "end", "showAs"};
            });
            List<ProviderCalendarEvent> events = new ArrayList<>();
            if (page == null || page.getValue() == null) {
                return List.of();
            }
            for (Event event : page.getValue()) {
                events.add(map(event));
            }
            return List.copyOf(events);
        } catch (ApiException ex) {
            String retryAfter = null;
            if (ex.getResponseHeaders() != null && ex.getResponseHeaders().containsKey("Retry-After")) {
                var values = ex.getResponseHeaders().get("Retry-After");
                if (values != null && !values.isEmpty()) {
                    retryAfter = values.iterator().next();
                }
            }
            int status = ex.getResponseStatusCode();
            throw new GraphProviderException(status, "graph provider error", retryAfter);
        } catch (RuntimeException ex) {
            if (ex instanceof GraphProviderException failure) {
                throw failure;
            }
            throw new GraphProviderException(0, "graph provider timeout or transport failure", null);
        }
    }

    static ProviderCalendarEvent map(Event event) {
        List<String> attendees = new ArrayList<>();
        if (event.getAttendees() != null) {
            for (Attendee attendee : event.getAttendees()) {
                if (attendee.getEmailAddress() != null
                        && attendee.getEmailAddress().getAddress() != null) {
                    attendees.add(attendee.getEmailAddress().getAddress());
                }
            }
        }
        String joinUrl =
                event.getOnlineMeeting() == null ? "" : event.getOnlineMeeting().getJoinUrl();
        String body = event.getBody() == null ? "" : event.getBody().getContent();
        String start = event.getStart() == null ? "" : event.getStart().getDateTime();
        String end = event.getEnd() == null ? "" : event.getEnd().getDateTime();
        String showAs = event.getShowAs() == null ? "" : event.getShowAs().toString();
        return new ProviderCalendarEvent(
                event.getId() == null ? "" : event.getId(),
                event.getSubject(),
                start,
                end,
                showAs,
                attendees,
                joinUrl == null ? "" : joinUrl,
                body == null ? "" : body);
    }

    private static final class StaticTokenProvider implements AccessTokenProvider {

        private final String token;
        private final AllowedHostsValidator validator = new AllowedHostsValidator("graph.microsoft.com");

        private StaticTokenProvider(String token) {
            this.token = token;
        }

        @Override
        public String getAuthorizationToken(URI uri, Map<String, Object> additionalAuthenticationContext) {
            return token;
        }

        @Override
        public AllowedHostsValidator getAllowedHostsValidator() {
            return validator;
        }
    }
}
