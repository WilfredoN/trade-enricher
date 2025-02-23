package com.capybara.trade.enricher.controller;

import com.capybara.trade.enricher.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = TradeController.class)
class TradeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TradeService tradeService;

    @Test
    void testHandleTradeJson() {
        String jsonInput = "[{\"date\":\"20230101\",\"productId\":\"2\",\"currency\":\"USD\",\"price\":100.0}]";
        String enrichedJson = "enriched json response";
        when(tradeService.enrichTrades(eq(jsonInput), eq("application/json")))
                .thenReturn(Mono.just(enrichedJson));
        webTestClient.post()
                .uri("/api/v1/trade")
                .header("Content-Type", "application/json")
                .bodyValue(jsonInput)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "application/json")
                .expectBody(String.class).isEqualTo(enrichedJson);
    }

    @Test
    void testHandleTradeXml() {
        String xmlInput = "<trades><trade><date>20230101</date><productId>2</productId><currency>USD</currency><price>100.0</price></trade></trades>";
        String enrichedXml = "enriched xml response";
        when(tradeService.enrichTrades(eq(xmlInput), eq("application/xml")))
                .thenReturn(Mono.just(enrichedXml));
        webTestClient.post()
                .uri("/api/v1/trade")
                .header("Content-Type", "application/xml")
                .bodyValue(xmlInput)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "application/xml")
                .expectBody(String.class).isEqualTo(enrichedXml);
    }

    @Test
    void testHandleTradeUnsupportedContentType() {
        String input = "some data";
        webTestClient.post()
                .uri("/api/v1/trade")
                .header("Content-Type", "text/plain")
                .bodyValue(input)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .expectBody(String.class)
                .isEqualTo("Unsupported Content-Type");
    }

    @Test
    void testHandleStreamingTrade() {
        String csvInput = "header\n20230101,2,USD,100.0";
        DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
        DataBuffer buffer = factory.wrap(csvInput.getBytes());
        Flux<DataBuffer> flux = Flux.just(buffer);
        String enrichedCsv = "enriched csv response";
        when(tradeService.enrichStreamingTrades(any()))
                .thenReturn(Mono.just(enrichedCsv));
        webTestClient.post()
                .uri("/api/v1/trade/stream")
                .header("Content-Type", "text/csv")
                .body(flux, DataBuffer.class)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", "text/csv")
                .expectBody(String.class).isEqualTo(enrichedCsv);
    }
}