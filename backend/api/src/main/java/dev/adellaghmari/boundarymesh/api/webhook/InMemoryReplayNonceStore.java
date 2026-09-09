package dev.adellaghmari.boundarymesh.api.webhook;

import dev.adellaghmari.boundarymesh.application.webhook.ReplayNonceStore;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryReplayNonceStore implements ReplayNonceStore {

    private final ConcurrentHashMap<String, Instant> seen = new ConcurrentHashMap<>();

    @Override
    public boolean recordIfAbsent(String nonceHash, Instant expiresAt, Instant now) {
        seen.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        Instant existing = seen.putIfAbsent(nonceHash, expiresAt);
        return existing == null;
    }
}
