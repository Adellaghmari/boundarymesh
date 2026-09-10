package dev.adellaghmari.boundarymesh.application.microsoft;

import java.util.List;

public record ProviderCalendarEvent(
        String providerId,
        String subject,
        String startTime,
        String endTime,
        String availability,
        List<String> attendees,
        String onlineMeetingUrl,
        String body) {}
