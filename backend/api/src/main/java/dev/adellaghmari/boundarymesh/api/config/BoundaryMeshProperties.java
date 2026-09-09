package dev.adellaghmari.boundarymesh.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "boundarymesh")
public record BoundaryMeshProperties(
        @NotBlank String serviceName,
        boolean devEvaluateEnabled,
        @Pattern(regexp = "[a-z0-9._-]+") String policyVersion) {}
