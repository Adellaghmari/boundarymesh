package dev.adellaghmari.boundarymesh.worker.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class SyntheticProviderServer {

    private final WireMockServer server;

    public SyntheticProviderServer(
            @org.springframework.beans.factory.annotation.Value("${boundarymesh.worker.wiremock-port:8099}") int port) {
        this.server =
                new WireMockServer(WireMockConfiguration.options().port(port).gzipDisabled(true));
    }

    @PostConstruct
    public void start() {
        if (!server.isRunning()) {
            server.start();
        }
        stubAll();
    }

    public void resetScenarios() {
        server.resetScenarios();
        server.resetRequests();
    }

    private void stubAll() {
        stubConnector("/covecrm/activities");
        stubConnector("/lumenway/fulfillments");
        stubConnector("/rivertide/settlements");
    }

    private void stubConnector(String base) {
        server.stubFor(post(urlPathEqualTo(base))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"accepted\"}")));
        server.stubFor(
                post(urlPathEqualTo(base + "/400")).willReturn(aResponse().withStatus(400)));
        server.stubFor(
                post(urlPathEqualTo(base + "/401")).willReturn(aResponse().withStatus(401)));
        server.stubFor(
                post(urlPathEqualTo(base + "/403")).willReturn(aResponse().withStatus(403)));
        server.stubFor(post(urlPathEqualTo(base + "/429"))
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "0")));
        server.stubFor(
                post(urlPathEqualTo(base + "/500")).willReturn(aResponse().withStatus(500)));
        server.stubFor(
                post(urlPathEqualTo(base + "/503")).willReturn(aResponse().withStatus(503)));
        server.stubFor(post(urlPathEqualTo(base + "/timeout"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(5_000)));
        server.stubFor(post(urlPathEqualTo(base + "/429-then-success"))
                .inScenario(base + "-429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("ok")
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "0")));
        server.stubFor(post(urlPathEqualTo(base + "/429-then-success"))
                .inScenario(base + "-429")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse().withStatus(200).withBody("{\"status\":\"accepted\"}")));
        server.stubFor(post(urlPathMatching(base + ".*"))
                .withRequestBody(containing("ada.lovelace@example.test"))
                .willReturn(aResponse().withStatus(409).withBody("{\"error\":\"sensitive-source-rejected\"}")));
    }

    public int port() {
        return server.port();
    }

    public WireMockServer server() {
        return server;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.port();
    }

    @PreDestroy
    public void stop() {
        if (server.isRunning()) {
            server.stop();
        }
    }
}
