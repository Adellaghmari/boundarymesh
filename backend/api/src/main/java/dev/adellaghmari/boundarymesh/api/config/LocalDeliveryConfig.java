package dev.adellaghmari.boundarymesh.api.config;

import dev.adellaghmari.boundarymesh.api.delivery.HttpWorkerDeliveryPort;
import dev.adellaghmari.boundarymesh.application.connector.ConnectorSafetyPort;
import dev.adellaghmari.boundarymesh.application.connector.LocalConnectorSafetyPort;
import dev.adellaghmari.boundarymesh.application.delivery.DeliveryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("local")
public class LocalDeliveryConfig {

    @Bean
    ConnectorSafetyPort connectorSafetyPort() {
        return new LocalConnectorSafetyPort();
    }

    @Bean
    DeliveryPort httpWorkerDeliveryPort(RestClient.Builder builder, BoundaryMeshProperties properties) {
        return new HttpWorkerDeliveryPort(builder, properties.workerBaseUrl());
    }
}
