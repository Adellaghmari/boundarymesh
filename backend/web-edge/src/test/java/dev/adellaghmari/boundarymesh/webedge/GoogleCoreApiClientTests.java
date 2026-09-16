package dev.adellaghmari.boundarymesh.webedge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.IdTokenCredentials;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GoogleCoreApiClientTests {
    @Test
    void sendsOnlyServerHeldShortLivedIdentityTokenToTheSelectedCorePath() throws Exception {
        var capturedPath = new AtomicReference<String>();
        var capturedAuthorization = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"status\":\"VALID\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            IdTokenCredentials credentials = Mockito.mock(IdTokenCredentials.class);
            given(credentials.getAccessToken())
                    .willReturn(new AccessToken(
                            "synthetic-unit-token", Date.from(Instant.now().plusSeconds(300))));
            var client = new GoogleCoreApiClient(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                    credentials,
                    HttpClient.newHttpClient());
            var response = client.call("GET", "/api/v1/attestations/id/verify", null);
            assertEquals(200, response.status());
            assertEquals("/api/v1/attestations/id/verify", capturedPath.get());
            assertEquals("Bearer synthetic-unit-token", capturedAuthorization.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void boundsAnUnexpectedCoreResponseBeforeItReachesTheBrowser() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = new byte[65537];
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            IdTokenCredentials credentials = Mockito.mock(IdTokenCredentials.class);
            given(credentials.getAccessToken())
                    .willReturn(new AccessToken(
                            "synthetic-unit-token", Date.from(Instant.now().plusSeconds(300))));
            var client = new GoogleCoreApiClient(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                    credentials,
                    HttpClient.newHttpClient());
            assertThrows(CoreUnavailableException.class, () -> client.call("GET", "/safe-read", null));
        } finally {
            server.stop(0);
        }
    }
}
