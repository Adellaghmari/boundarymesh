package dev.adellaghmari.boundarymesh.application.connector;

import dev.adellaghmari.boundarymesh.domain.connector.ConnectorSafetySnapshot;

public interface ConnectorSafetyPort {

    ConnectorSafetySnapshot snapshot(String destination);
}
