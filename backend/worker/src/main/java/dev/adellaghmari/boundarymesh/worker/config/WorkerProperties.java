package dev.adellaghmari.boundarymesh.worker.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boundarymesh.worker")
public record WorkerProperties(
        int wiremockPort,
        int retryMaxAttempts,
        Duration retryWait,
        Duration retryAfterCap,
        Duration timeout,
        int circuitWindow,
        Duration circuitOpenWait) {}
