package dev.adellaghmari.boundarymesh.gcp.secret;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import java.io.IOException;
import java.util.Objects;

/**
 * Google Secret Manager reader. Constructed only when a production composition explicitly enables
 * it. Default tests never create this client.
 */
public final class GoogleSecretAccessor implements SecretAccessor, AutoCloseable {

    private final SecretManagerServiceClient client;

    public GoogleSecretAccessor(SecretManagerServiceClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public static GoogleSecretAccessor connect() {
        try {
            return new GoogleSecretAccessor(SecretManagerServiceClient.create());
        } catch (IOException ex) {
            throw SecretAccessException.unavailable("secret-manager");
        }
    }

    @Override
    public byte[] access(SecretResourceName name) {
        Objects.requireNonNull(name, "name");
        try {
            var response = client.accessSecretVersion(name.resourceName());
            if (response == null
                    || !response.hasPayload()
                    || response.getPayload().getData().isEmpty()) {
                throw SecretAccessException.notFound(name.secretId());
            }
            return response.getPayload().getData().toByteArray();
        } catch (SecretAccessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw translate(name.secretId(), ex);
        }
    }

    static SecretAccessException translate(String secretId, RuntimeException error) {
        if (error instanceof ApiException api) {
            StatusCode.Code code = api.getStatusCode().getCode();
            if (code == StatusCode.Code.NOT_FOUND) {
                return SecretAccessException.notFound(secretId);
            }
            if (code == StatusCode.Code.PERMISSION_DENIED) {
                return SecretAccessException.denied(secretId);
            }
        }
        return SecretAccessException.unavailable(secretId);
    }

    @Override
    public void close() {
        client.close();
    }
}
