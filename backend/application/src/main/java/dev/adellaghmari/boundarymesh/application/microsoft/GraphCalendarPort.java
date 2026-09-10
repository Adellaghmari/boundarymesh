package dev.adellaghmari.boundarymesh.application.microsoft;

import java.util.List;

public interface GraphCalendarPort {

    List<ProviderCalendarEvent> readUpcoming(int limit);
}
