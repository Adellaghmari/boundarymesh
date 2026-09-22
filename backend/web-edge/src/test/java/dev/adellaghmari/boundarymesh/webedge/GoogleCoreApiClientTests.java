package dev.adellaghmari.boundarymesh.webedge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.IdTokenCredentials;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GoogleCoreApiClientTests {
    private static final Duration TEST_REQUEST_TIMEOUT = Duration.ofSeconds(2);

    @Test
    void productionRequestsRemainBoundedAtThirtySeconds() {
        assertEquals(Duration.ofSeconds(30), GoogleCoreApiClient.PRODUCTION_REQUEST_TIMEOUT);
    }

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
                    HttpClient.newHttpClient(),
                    TEST_REQUEST_TIMEOUT);
            var response = client.call("GET", "/api/v1/attestations/id/verify", null);
            assertEquals(200, response.status());
            assertEquals("/api/v1/attestations/id/verify", capturedPath.get());
            assertEquals("Bearer synthetic-unit-token", capturedAuthorization.get());
            verify(credentials).refreshIfExpired();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void waitsForAValidDelayedCoreResponseWithinTheBoundedTimeout() throws Exception {
        HttpServer server = responseServer(120, 200, "{\"status\":\"VALID\"}");
        try {
            var client = client(server, Duration.ofMillis(750));
            var response = client.call("GET", "/safe-read", null);
            assertEquals(200, response.status());
            assertEquals("{\"status\":\"VALID\"}", response.body());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void failsClosedWhenTheCoreExceedsTheConfiguredTimeout() throws Exception {
        HttpServer server = responseServer(250, 200, "{\"status\":\"VALID\"}");
        try {
            var client = client(server, Duration.ofMillis(50));
            assertThrows(CoreUnavailableException.class, () -> client.call("GET", "/safe-read", null));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void nonTimeoutIoFailureStillFailsClosed() throws Exception {
        int unusedPort;
        try (var socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        var client = new GoogleCoreApiClient(
                URI.create("http://127.0.0.1:" + unusedPort),
                credentials(),
                HttpClient.newBuilder().connectTimeout(Duration.ofMillis(250)).build(),
                Duration.ofSeconds(1));
        assertThrows(CoreUnavailableException.class, () -> client.call("GET", "/safe-read", null));
    }

    @Test
    void preservesCoreClientAndServerResponses() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/missing", exchange -> respond(exchange, 404, "{\"error_code\":\"NOT_FOUND\"}"));
        server.createContext("/failure", exchange -> respond(exchange, 503, "{\"error_code\":\"UNAVAILABLE\"}"));
        server.start();
        try {
            var client = client(server, TEST_REQUEST_TIMEOUT);
            assertEquals(404, client.call("GET", "/missing", null).status());
            assertEquals(503, client.call("GET", "/failure", null).status());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsUnboundedOrInvalidRequestTimeouts() {
        var base = URI.create("https://example.invalid");
        var credentials = credentials();
        var http = HttpClient.newHttpClient();
        assertThrows(IllegalArgumentException.class, () -> new GoogleCoreApiClient(base, credentials, http, null));
        assertThrows(
                IllegalArgumentException.class, () -> new GoogleCoreApiClient(base, credentials, http, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GoogleCoreApiClient(base, credentials, http, Duration.ofMillis(-1)));
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
                    HttpClient.newHttpClient(),
                    TEST_REQUEST_TIMEOUT);
            assertThrows(CoreUnavailableException.class, () -> client.call("GET", "/safe-read", null));
        } finally {
            server.stop(0);
        }
    }

    private static GoogleCoreApiClient client(HttpServer server, Duration timeout) {
        return new GoogleCoreApiClient(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                credentials(),
                HttpClient.newHttpClient(),
                timeout);
    }

    private static IdTokenCredentials credentials() {
        IdTokenCredentials credentials = Mockito.mock(IdTokenCredentials.class);
        given(credentials.getAccessToken())
                .willReturn(new AccessToken(
                        "synthetic-unit-token", Date.from(Instant.now().plusSeconds(300))));
        return credentials;
    }

    private static HttpServer responseServer(long delayMillis, int status, String response) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(delayMillis);
                respond(exchange, status, response);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // A timed-out client closes the local exchange before the delayed response is written.
            }
        });
        server.start();
        return server;
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String response)
            throws IOException {
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
