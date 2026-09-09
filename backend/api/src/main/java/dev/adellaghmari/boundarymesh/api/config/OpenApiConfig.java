package dev.adellaghmari.boundarymesh.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI boundaryMeshOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("BoundaryMesh API")
                                .version("v1-phase3")
                                .description(
                                        "Local JWT resource server and QuayForge HMAC webhook. Not Entra, not Graph. OpenAPI is not runtime contract enforcement."))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearer-jwt",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSecuritySchemes(
                                "quayforge-hmac",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Velora-Signature")
                                        .description("QuayForge HMAC-SHA256 header v1=<hex>. Not OAuth.")));
    }
}
