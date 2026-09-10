package dev.adellaghmari.boundarymesh.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "boundarymesh")
public record BoundaryMeshProperties(
        @NotBlank String serviceName,
        boolean devEvaluateEnabled,
        boolean devSecurityToolsEnabled,
        @Pattern(regexp = "[a-z0-9._-]+") String policyVersion,
        String workerBaseUrl,
        @Valid @NotNull Security security,
        @Valid @NotNull Webhook webhook,
        @Valid @NotNull Entra entra) {

    public record Security(@NotBlank String issuer, @NotBlank String audience, @NotNull Duration accessTokenTtl) {}

    public record Webhook(
            @NotNull Duration freshnessWindow, @Min(1024) @Max(262144) int maxBodyBytes, String quayforgeSecret) {}

    public record Entra(
            String tenantId,
            String clientId,
            String clientSecret,
            String redirectUri,
            String frontendOrigin,
            String graphMode) {

        public boolean configured() {
            return notBlank(tenantId) && notBlank(clientId) && notBlank(clientSecret);
        }

        public boolean liveGraph() {
            return configured() && "LIVE".equalsIgnoreCase(graphMode);
        }

        public String modeLabel() {
            if (liveGraph()) {
                return "LIVE_CONFIGURED";
            }
            return configured() ? "CONTRACT_HARNESS" : "NOT_CONFIGURED";
        }

        public String authorityMode() {
            return configured() ? "COMMON" : "NONE";
        }

        private static boolean notBlank(String value) {
            return value != null && !value.isBlank();
        }
    }
}
