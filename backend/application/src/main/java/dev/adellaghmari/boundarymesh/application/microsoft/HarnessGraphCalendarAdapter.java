package dev.adellaghmari.boundarymesh.application.microsoft;

import java.util.List;

public final class HarnessGraphCalendarAdapter implements GraphCalendarPort {

    @Override
    public List<ProviderCalendarEvent> readUpcoming(int limit) {
        int size = Math.max(1, Math.min(limit, 5));
        return List.of(
                        new ProviderCalendarEvent(
                                "harness-event-1",
                                "Customer renewal workshop",
                                "2026-09-10T13:00:00Z",
                                "2026-09-10T13:30:00Z",
                                "busy",
                                List.of("ada.lovelace@example.test"),
                                "https://teams.microsoft.com/l/meetup-join/synthetic",
                                "Do not expose this body."),
                        new ProviderCalendarEvent(
                                "harness-event-2",
                                "Internal finance sync",
                                "2026-09-10T15:00:00Z",
                                "2026-09-10T15:25:00Z",
                                "tentative",
                                List.of(),
                                "",
                                ""))
                .subList(0, Math.min(size, 2));
    }
}
