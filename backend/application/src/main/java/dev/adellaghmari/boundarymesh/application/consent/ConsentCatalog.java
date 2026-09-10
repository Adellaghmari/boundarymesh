package dev.adellaghmari.boundarymesh.application.consent;

import dev.adellaghmari.boundarymesh.domain.consent.ConnectorOperation;
import dev.adellaghmari.boundarymesh.domain.consent.ConsentGrant;
import java.util.List;
import java.util.Optional;

public interface ConsentCatalog {

    Optional<ConsentGrant> find(String destination, ConnectorOperation operation);

    List<ConsentGrant> list();
}
