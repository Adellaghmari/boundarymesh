package dev.adellaghmari.boundarymesh.api.microsoft;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.adellaghmari.boundarymesh.application.microsoft.GraphProviderException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class GraphCalendarHttpAdapterTest {

    @RegisterExtension
    static WireMockExtension graph = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void mapsSuccessfulCalendarView() {
        graph.stubFor(
                get(urlPathEqualTo("/v1.0/me/calendarView"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                """
                                {"value":[{"id":"e1","subject":"Secret","start":{"dateTime":"2026-09-10T13:00:00"},"end":{"dateTime":"2026-09-10T14:00:00"},"showAs":"busy","attendees":[{"emailAddress":{"address":"a@b.test"}}],"onlineMeeting":{"joinUrl":"https://teams.microsoft.com/x"},"body":{"content":"nope"}}]}
                                """)));
        var events = new GraphCalendarHttpAdapter(graph.baseUrl(), () -> "token").readUpcoming(5);
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().subject()).isEqualTo("Secret");
        assertThat(events.getFirst().attendees()).containsExactly("a@b.test");
    }

    @Test
    void emptyCalendarIsEmptyList() {
        graph.stubFor(get(urlPathEqualTo("/v1.0/me/calendarView"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"value\":[]}")));
        assertThat(new GraphCalendarHttpAdapter(graph.baseUrl(), () -> "token").readUpcoming(5))
                .isEmpty();
    }

    @Test
    void unauthorizedIs401() {
        graph.stubFor(get(urlPathEqualTo("/v1.0/me/calendarView"))
                .willReturn(aResponse().withStatus(401)));
        assertThatThrownBy(() -> new GraphCalendarHttpAdapter(graph.baseUrl(), () -> "token").readUpcoming(5))
                .isInstanceOf(GraphProviderException.class)
                .extracting(ex -> ((GraphProviderException) ex).status())
                .isEqualTo(401);
    }

    @Test
    void forbiddenIs403() {
        graph.stubFor(get(urlPathEqualTo("/v1.0/me/calendarView"))
                .willReturn(aResponse().withStatus(403)));
        assertThatThrownBy(() -> new GraphCalendarHttpAdapter(graph.baseUrl(), () -> "token").readUpcoming(5))
                .isInstanceOf(GraphProviderException.class)
                .extracting(ex -> ((GraphProviderException) ex).status())
                .isEqualTo(403);
    }

    @Test
    void tooManyRequestsHonorsRetryAfterHeader() {
        graph.stubFor(get(urlPathEqualTo("/v1.0/me/calendarView"))
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "0")));
        assertThatThrownBy(() -> new GraphCalendarHttpAdapter(graph.baseUrl(), () -> "token").readUpcoming(5))
                .isInstanceOf(GraphProviderException.class)
                .satisfies(ex -> {
                    GraphProviderException failure = (GraphProviderException) ex;
                    assertThat(failure.status()).isEqualTo(429);
                    assertThat(failure.retryAfter()).isEqualTo("0");
                    assertThat(failure.retryable()).isTrue();
                });
    }

    @Test
    void serverErrorIs5xx() {
        graph.stubFor(get(urlPathEqualTo("/v1.0/me/calendarView"))
                .willReturn(aResponse().withStatus(503)));
        assertThatThrownBy(() -> new GraphCalendarHttpAdapter(graph.baseUrl(), () -> "token").readUpcoming(5))
                .isInstanceOf(GraphProviderException.class)
                .extracting(ex -> ((GraphProviderException) ex).status())
                .isEqualTo(503);
    }

    @Test
    void malformedProviderJsonIsRejected() {
        graph.stubFor(get(urlPathEqualTo("/v1.0/me/calendarView"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));
        assertThatThrownBy(() -> new GraphCalendarHttpAdapter(graph.baseUrl(), () -> "token").readUpcoming(5))
                .isInstanceOf(GraphProviderException.class)
                .extracting(ex -> ((GraphProviderException) ex).status())
                .isEqualTo(502);
    }
}
