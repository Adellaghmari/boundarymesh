package dev.adellaghmari.boundarymesh.application.attestation;

import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer.CrossingResult;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationId;
import dev.adellaghmari.boundarymesh.domain.attestation.AttestationVersion;
import dev.adellaghmari.boundarymesh.domain.attestation.ExchangeAttestation;
import dev.adellaghmari.boundarymesh.domain.attestation.SignerKeyReference;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainHead;
import dev.adellaghmari.boundarymesh.domain.audit.AuditChainId;
import dev.adellaghmari.boundarymesh.domain.delivery.DeliveryResult;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AttestationFactory {

    private final Clock clock;
    private final AttestationSigner signer;

    public AttestationFactory(Clock clock, AttestationSigner signer) {
        this.clock = Objects.requireNonNull(clock);
        this.signer = Objects.requireNonNull(signer);
    }

    public ExchangeAttestation create(
            CrossingResult crossing,
            String source,
            String destination,
            String principalReference,
            String authenticationMethod,
            DeliveryResult delivery,
            AuditChainId chainId,
            AuditChainHead head) {
        Map<String, String> controls = new LinkedHashMap<>();
        crossing.vector()
                .asList()
                .forEach(control ->
                        controls.put(control.kind().name(), control.status().name()));
        SignerKeyReference key = signer.key();
        ExchangeAttestation unsigned = new ExchangeAttestation(
                AttestationId.random(),
                AttestationVersion.V1,
                crossing.decision().transactionId().value(),
                chainId.value(),
                source,
                destination,
                clock.instant(),
                principalReference,
                authenticationMethod,
                controls,
                crossing.contract().reference() == null
                        ? null
                        : crossing.contract().reference().id().value(),
                crossing.contract().reference() == null
                        ? null
                        : crossing.contract().reference().version().value(),
                crossing.contract().classification(),
                crossing.egress().policyId() == null
                        ? null
                        : crossing.egress().policyId().value(),
                crossing.egress().summary().transformationCount(),
                crossing.credentialReference() == null
                        ? null
                        : crossing.credentialReference().display(),
                crossing.consent().status().name(),
                delivery == null ? null : delivery.routeId(),
                delivery == null ? "NOT_ATTEMPTED" : delivery.kind().name(),
                crossing.decision().outcome(),
                chainId,
                head.sequence(),
                head.headHash(),
                key,
                dev.adellaghmari.boundarymesh.domain.attestation.AttestationStatus.UNSIGNED,
                dev.adellaghmari.boundarymesh.domain.attestation.AttestationSignature.none());
        try {
            var signature = signer.sign(AttestationCanonicalizer.bytes(unsigned));
            if (signature == null || !signature.present()) {
                return unsigned.unsigned();
            }
            return unsigned.signed(signature);
        } catch (RuntimeException ignored) {
            return unsigned.unsigned();
        }
    }
}
