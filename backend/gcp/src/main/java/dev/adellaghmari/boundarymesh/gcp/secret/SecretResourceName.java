package dev.adellaghmari.boundarymesh.gcp.secret;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable Secret Manager resource coordinates. Holds identifiers only — never payload.
 */
public final class SecretResourceName {

    public static final String LATEST = "latest";

    private static final Pattern PROJECT = Pattern.compile("^[a-z][a-z0-9-]{4,28}[a-z0-9]$");
    private static final Pattern SECRET_ID = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_-]{0,254}$");
    private static final Pattern VERSION = Pattern.compile("^(latest|[1-9][0-9]{0,19})$");

    private final String projectId;
    private final String secretId;
    private final String version;

    public SecretResourceName(String projectId, String secretId, String version) {
        this.projectId = requireToken(projectId, PROJECT);
        this.secretId = requireToken(secretId, SECRET_ID);
        this.version = requireToken(version, VERSION);
    }

    public static SecretResourceName latest(String projectId, String secretId) {
        return new SecretResourceName(projectId, secretId, LATEST);
    }

    public String projectId() {
        return projectId;
    }

    public String secretId() {
        return secretId;
    }

    public String version() {
        return version;
    }

    public String resourceName() {
        return "projects/" + projectId + "/secrets/" + secretId + "/versions/" + version;
    }

    @Override
    public String toString() {
        return resourceName();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SecretResourceName that)) {
            return false;
        }
        return projectId.equals(that.projectId) && secretId.equals(that.secretId) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, secretId, version);
    }

    private static String requireToken(String value, Pattern pattern) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw SecretAccessException.invalidReference();
        }
        return value;
    }
}
