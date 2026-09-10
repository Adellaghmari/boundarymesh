package dev.adellaghmari.boundarymesh.application.attestation;

import dev.adellaghmari.boundarymesh.domain.attestation.AttestationId;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import java.util.List;
import java.util.Optional;

public interface AttestationRepository {

    void save(ExchangeAttestation attestation);

    Optional<ExchangeAttestation> find(AttestationId id);

    List<ExchangeAttestation> list(int limit);
}
