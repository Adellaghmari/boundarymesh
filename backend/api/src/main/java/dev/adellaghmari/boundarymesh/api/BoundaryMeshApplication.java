package dev.adellaghmari.boundarymesh.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(
        excludeName = {
            "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
            "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration"
        })
@ConfigurationPropertiesScan
public class BoundaryMeshApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoundaryMeshApplication.class, args);
    }
}
