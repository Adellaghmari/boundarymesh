package dev.adellaghmari.boundarymesh.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
                                .version("v1-phase1")
                                .description(
                                        "Phase 1 foundation only. OpenAPI 3.1 document generation is not runtime contract enforcement."));
    }
}
