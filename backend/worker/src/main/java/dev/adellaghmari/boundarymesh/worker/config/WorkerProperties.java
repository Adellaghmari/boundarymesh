package dev.adellaghmari.boundarymesh.worker.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boundarymesh.worker")
public record WorkerProperties(
        int wiremockPort,
        String covecrmBaseUrl,
        String lumenwayBaseUrl,
        String rivertideBaseUrl,
        int retryMaxAttempts,
        Duration retryWait,
        Duration retryAfterCap,
        Duration timeout,
        int circuitWindow,
        Duration circuitOpenWait) {

    public String providerBaseUrl(String destination) {
        return switch (destination == null ? "" : destination.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "COVECRM" -> clean(covecrmBaseUrl);
            case "LUMENWAY" -> clean(lumenwayBaseUrl);
            case "RIVERTIDE" -> clean(rivertideBaseUrl);
            default -> "";
        };
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
