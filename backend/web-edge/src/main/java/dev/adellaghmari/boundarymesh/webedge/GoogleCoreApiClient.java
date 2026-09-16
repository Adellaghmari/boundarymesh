package dev.adellaghmari.boundarymesh.webedge;

import com.google.auth.oauth2.IdTokenCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Only the trusted BFF runtime holds the short-lived Google-signed Cloud Run ID token. */
final class GoogleCoreApiClient implements CoreApiClient {
    private final URI base;
    private final IdTokenCredentials credentials;
    private final HttpClient client;

    GoogleCoreApiClient(URI base, IdTokenCredentials credentials, HttpClient client) {
        this.base = base;
        this.credentials = credentials;
        this.client = client;
    }

    @Override
    public CoreResponse call(String method, String path, byte[] body) {
        try {
            credentials.refreshIfExpired();
            String token = credentials.getAccessToken().getTokenValue();
            var request = HttpRequest.newBuilder(base.resolve(path))
                    .timeout(Duration.ofSeconds(12))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .method(
                            method,
                            body == null
                                    ? HttpRequest.BodyPublishers.noBody()
                                    : HttpRequest.BodyPublishers.ofByteArray(body))
                    .header("Content-Type", "application/json")
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream stream = response.body()) {
                byte[] bytes = stream.readNBytes(65537);
                if (bytes.length > 65536) {
                    throw new CoreUnavailableException();
                }
                return new CoreResponse(response.statusCode(), new String(bytes, StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            throw new CoreUnavailableException();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CoreUnavailableException();
        } catch (RuntimeException ex) {
            throw new CoreUnavailableException();
        }
    }
}
