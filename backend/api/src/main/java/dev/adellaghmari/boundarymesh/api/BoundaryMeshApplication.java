package dev.adellaghmari.boundarymesh.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(
        excludeName = "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration")
@ConfigurationPropertiesScan
public class BoundaryMeshApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoundaryMeshApplication.class, args);
    }
}
