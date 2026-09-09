package dev.adellaghmari.boundarymesh.domain.exception;

/**
 * Local configuration is invalid. Distinct from security REJECT and from adapter failures.
 */
public final class ConfigurationException extends DomainException {

    public ConfigurationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
