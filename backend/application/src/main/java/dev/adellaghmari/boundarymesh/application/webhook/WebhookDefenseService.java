package dev.adellaghmari.boundarymesh.application.webhook;

import dev.adellaghmari.boundarymesh.application.BoundaryEnforcementEngine;
import dev.adellaghmari.boundarymesh.application.crossing.CrossingComposer;
import dev.adellaghmari.boundarymesh.domain.BoundaryDecision;
import dev.adellaghmari.boundarymesh.domain.ControlKind;
import dev.adellaghmari.boundarymesh.domain.ControlResult;
import dev.adellaghmari.boundarymesh.domain.ControlVector;
import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.Sha256Reference;
import dev.adellaghmari.boundarymesh.domain.TransactionId;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic signed-webhook defense. Order: headers → HMAC over raw bytes → freshness → replay → parse
 * → contract → egress → idempotency → engine. Security failures do not inspect payload fields.
 */
public final class WebhookDefenseService {

    private static final Pattern EVENT_ID = Pattern.compile("\"event_id\"\\s*:\\s*\"([A-Za-z0-9._:-]{1,128})\"");

    private final BoundaryEnforcementEngine engine;
    private final WebhookSecretProvider secrets;
    private final ReplayNonceStore replayNonceStore;
    private final IdempotencyStore idempotencyStore;
    private final FreshnessPolicy freshnessPolicy;
    private final Clock clock;
    private final CrossingComposer composer;

    public WebhookDefenseService(
            BoundaryEnforcementEngine engine,
            WebhookSecretProvider secrets,
            ReplayNonceStore replayNonceStore,
            IdempotencyStore idempotencyStore,
            FreshnessPolicy freshnessPolicy,
            Clock clock,
            CrossingComposer composer) {
        this.engine = Objects.requireNonNull(engine);
        this.secrets = Objects.requireNonNull(secrets);
        this.replayNonceStore = Objects.requireNonNull(replayNonceStore);
        this.idempotencyStore = Objects.requireNonNull(idempotencyStore);
        this.freshnessPolicy = Objects.requireNonNull(freshnessPolicy);
        this.clock = Objects.requireNonNull(clock);
        this.composer = Objects.requireNonNull(composer);
    }

    public WebhookDefenseResult evaluate(TransactionId transactionId, QuayForgeWebhookRequest request) {
        Instant now = clock.instant();
        ControlResult authenticity = authenticity(request);
        if (authenticity.blocksCrossing()) {
            return decide(
                    transactionId,
                    authenticity,
                    ControlResult.notApplicable(ControlKind.FRESHNESS),
                    ControlResult.notApplicable(ControlKind.REPLAY),
                    false,
                    null);
        }

        ControlResult freshness = freshness(request.timestamp(), now);
        if (freshness.blocksCrossing()) {
            return decide(
                    transactionId,
                    authenticity,
                    freshness,
                    ControlResult.notApplicable(ControlKind.REPLAY),
                    false,
                    null);
        }

        String nonceHash = Sha256Reference.ofUtf8(request.connectorId() + ":" + request.nonce());
        boolean firstSeen = replayNonceStore.recordIfAbsent(
                nonceHash, freshnessPolicy.upperBound(now).plus(freshnessPolicy.window()), now);
        if (!firstSeen) {
            return decide(
                    transactionId,
                    authenticity,
                    freshness,
                    ControlResult.fail(
                            ControlKind.REPLAY,
                            ReasonCode.REPLAY_DETECTED,
                            "Replay defense failed. A prior nonce was already recorded in the protected window."),
                    false,
                    null);
        }

        ControlResult replayPass = new ControlResult(
                ControlKind.REPLAY,
                dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                java.util.List.of(),
                "No prior nonce within the protected replay window.",
                nonceHash.substring(0, Math.min(16, nonceHash.length())));

        Optional<String> eventId = extractEventId(request.rawBody());
        if (eventId.isEmpty()) {
            ControlVector vector = vector(
                    authenticity,
                    freshness,
                    replayPass,
                    ControlResult.fail(
                            ControlKind.CONTRACT,
                            ReasonCode.CONTRACT_INVALID,
                            "Webhook JSON is missing a usable event_id."));
            BoundaryDecision decision = engine.decide(transactionId, vector);
            return new WebhookDefenseResult(decision, vector, false, null);
        }

        String eventKey = Sha256Reference.ofUtf8(request.connectorId() + ":" + eventId.get());
        Optional<BoundaryDecision> prior = idempotencyStore.find(eventKey);
        if (prior.isPresent()) {
            ControlVector vector =
                    vector(authenticity, freshness, replayPass, ControlResult.pass(ControlKind.CONTRACT));
            return new WebhookDefenseResult(prior.get(), vector, true, eventId.get());
        }

        CrossingComposer.CrossingResult composed = composer.compose(CrossingComposer.webhookAfterSecurity(
                        new String(request.rawBody(), StandardCharsets.UTF_8),
                        request.contractVersion(),
                        authenticity,
                        freshness,
                        replayPass)
                .withTransaction(transactionId));
        idempotencyStore.putIfAbsent(eventKey, composed.decision());
        return new WebhookDefenseResult(composed.decision(), composed.vector(), false, eventId.get());
    }

    private ControlResult authenticity(QuayForgeWebhookRequest request) {
        if (request.signatureHeader() == null || request.signatureHeader().isBlank()) {
            return ControlResult.fail(
                    ControlKind.AUTHENTICITY,
                    ReasonCode.AUTHENTICITY_MISSING_SIGNATURE,
                    "Webhook signature header is missing.");
        }
        if (!QuayForgeSigningContract.hasSignatureFormat(request.signatureHeader())) {
            return ControlResult.fail(
                    ControlKind.AUTHENTICITY,
                    ReasonCode.AUTHENTICITY_MISSING_SIGNATURE,
                    "Webhook signature header is malformed.");
        }
        byte[] secret = secrets.secretBytes(request.connectorId());
        String expected = QuayForgeSigningContract.signatureHeader(
                secret, request.timestamp(), request.nonce(), request.rawBody());
        if (!QuayForgeSigningContract.signaturesEqual(request.signatureHeader(), expected)) {
            return ControlResult.fail(
                    ControlKind.AUTHENTICITY,
                    ReasonCode.AUTHENTICITY_INVALID_SIGNATURE,
                    "Webhook HMAC did not match the raw-body contract.");
        }
        return new ControlResult(
                ControlKind.AUTHENTICITY,
                dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                java.util.List.of(),
                "HMAC-SHA256 matched the raw request body.",
                null);
    }

    private ControlResult freshness(String timestamp, Instant now) {
        long epochSeconds;
        try {
            epochSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            return ControlResult.fail(
                    ControlKind.FRESHNESS,
                    ReasonCode.FRESHNESS_EXPIRED,
                    "Signed timestamp is not a Unix epoch second.");
        }
        Instant signedAt = Instant.ofEpochSecond(epochSeconds);
        if (signedAt.isBefore(freshnessPolicy.lowerBound(now))) {
            return ControlResult.fail(
                    ControlKind.FRESHNESS,
                    ReasonCode.FRESHNESS_EXPIRED,
                    "Signed timestamp is older than the accepted freshness window.");
        }
        if (signedAt.isAfter(freshnessPolicy.upperBound(now))) {
            return ControlResult.fail(
                    ControlKind.FRESHNESS, ReasonCode.FRESHNESS_FUTURE, "Signed timestamp is too far in the future.");
        }
        return new ControlResult(
                ControlKind.FRESHNESS,
                dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                java.util.List.of(),
                "Signed timestamp is inside the ±" + freshnessPolicy.window().toSeconds() + "s window.",
                null);
    }

    private WebhookDefenseResult decide(
            TransactionId transactionId,
            ControlResult authenticity,
            ControlResult freshness,
            ControlResult replay,
            boolean duplicate,
            String eventId) {
        ControlVector vector =
                vector(authenticity, freshness, replay, ControlResult.notApplicable(ControlKind.CONTRACT));
        BoundaryDecision decision = engine.decide(transactionId, vector);
        return new WebhookDefenseResult(decision, vector, duplicate, eventId);
    }

    private static ControlVector vector(
            ControlResult authenticity, ControlResult freshness, ControlResult replay, ControlResult contract) {
        return ControlVector.of(
                new ControlResult(
                        ControlKind.IDENTITY,
                        dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                        java.util.List.of(),
                        "QuayForge connector identity is established by the signed webhook, not a JWT.",
                        null),
                new ControlResult(
                        ControlKind.AUTHORIZATION,
                        dev.adellaghmari.boundarymesh.domain.ControlStatus.PASS,
                        java.util.List.of(),
                        "QuayForge Orders is permitted for this synthetic settlement crossing.",
                        null),
                authenticity,
                freshness,
                replay,
                contract,
                ControlResult.pass(ControlKind.EGRESS),
                ControlResult.pass(ControlKind.CREDENTIAL_RESOLVABILITY),
                ControlResult.pass(ControlKind.CONNECTOR_SAFETY));
    }

    private static Optional<String> extractEventId(byte[] rawBody) {
        String json = new String(rawBody, StandardCharsets.UTF_8);
        Matcher matcher = EVENT_ID.matcher(json);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.ofNullable(matcher.group(1));
    }
}
