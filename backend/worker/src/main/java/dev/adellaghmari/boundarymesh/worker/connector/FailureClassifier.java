package dev.adellaghmari.boundarymesh.worker.connector;

import dev.adellaghmari.boundarymesh.domain.ReasonCode;
import dev.adellaghmari.boundarymesh.domain.delivery.ProviderFailureCategory;

public final class FailureClassifier {

    private FailureClassifier() {}

    public static ProviderFailureCategory category(int status) {
        return switch (status) {
            case 200, 201, 202, 204 -> ProviderFailureCategory.SUCCESS;
            case 400 -> ProviderFailureCategory.PERMANENT_REJECTION;
            case 401, 403 -> ProviderFailureCategory.CREDENTIAL_OR_CONSENT;
            case 429 -> ProviderFailureCategory.RATE_LIMITED;
            case 408, 500, 502, 503, 504 -> ProviderFailureCategory.TEMPORARY_FAILURE;
            default ->
                status >= 400 && status < 500
                        ? ProviderFailureCategory.PERMANENT_REJECTION
                        : ProviderFailureCategory.TEMPORARY_FAILURE;
        };
    }

    public static boolean retryable(int status) {
        return status == 408 || status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    public static ReasonCode reason(ProviderFailureCategory category) {
        return switch (category) {
            case PERMANENT_REJECTION -> ReasonCode.PROVIDER_PERMANENT_REJECTION;
            case CREDENTIAL_OR_CONSENT -> ReasonCode.CREDENTIAL_UNAVAILABLE;
            case RATE_LIMITED -> ReasonCode.PROVIDER_RATE_LIMITED;
            case TIMEOUT -> ReasonCode.PROVIDER_TEMPORARY_FAILURE;
            case CIRCUIT_OPEN -> ReasonCode.CONNECTOR_CIRCUIT_OPEN;
            case UNKNOWN_DESTINATION -> ReasonCode.CONNECTOR_UNKNOWN;
            case DUPLICATE -> ReasonCode.DELIVERY_DUPLICATE_SUPPRESSED;
            default -> ReasonCode.DELIVERY_RETRIES_EXHAUSTED;
        };
    }
}
