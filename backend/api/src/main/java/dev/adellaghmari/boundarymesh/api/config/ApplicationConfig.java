package dev.adellaghmari.boundarymesh.api.config;

import dev.adellaghmari.boundarymesh.application.BoundaryEnforcementEngine;
import dev.adellaghmari.boundarymesh.domain.PolicyVersion;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    BoundaryEnforcementEngine boundaryEnforcementEngine(Clock clock, BoundaryMeshProperties properties) {
        return new BoundaryEnforcementEngine(clock, new PolicyVersion(properties.policyVersion()));
    }
}
