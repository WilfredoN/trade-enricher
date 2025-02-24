package com.capybara.trade.enricher.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TradeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = webTestClient.mutate()
                .responseTimeout(java.time.Duration.ofSeconds(120))
                .build();
    }

    @Test
    public void testJsonTradeEnrichment() {
        String jsonInput = """
                [
                    {"date":"20230104","productId":"8","currency":"EUR","price":450.20},
                    {"date":"20230104","productId":"9","currency":"GBP","price":500.30}
                ]""";

        String response = webTestClient.post()
                .uri("/api/v1/trade")
                .header("Content-Type", "application/json")
                .bodyValue(jsonInput)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        if (response != null) {
            log.debug("JSON Response: {}", response);
            assertTrue(response.contains("\"date\":\"20230104\""));
            assertTrue(response.contains("\"productId\":\"8\""));
            assertTrue(response.contains("\"currency\":\"EUR\""));
            assertTrue(response.contains("\"price\":450.2"));
            assertTrue(response.contains("\"productName\":"));
        }
    }

    @Test
    public void testXmlTradeEnrichment() {
        String xmlInput = """
                <trades>
                    <trade><date>20230105</date><productId>10</productId><currency>USD</currency><price>550.40</price></trade>
                    <trade><date>20230105</date><productId>11</productId><currency>EUR</currency><price>600.50</price></trade>
                </trades>""";

        String response = webTestClient.post()
                .uri("/api/v1/trade")
                .header("Content-Type", "application/xml")
                .bodyValue(xmlInput)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/xml")
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();
        if (response != null) {
            assertTrue(response.contains("<date>20230105</date>"));
            assertTrue(response.contains("<productId>10</productId>"));
            assertTrue(response.contains("<currency>USD</currency>"));
            log.debug("XML Response: {}", response);
            assertEquals(2, response.split("<item>").length - 1);
            assertTrue(response.contains("<productName>"));
        }
    }

    @Test
    public void testCsvTradeEnrichment() {
        String csvInput =
                """
                        date,productId,currency,price
                        20230106,2,USD,700.60
                        20230106,3,EUR,800.70""";

        String response = webTestClient.post()
                .uri("/api/v1/trade")
                .header("Content-Type", "text/csv")
                .bodyValue(csvInput)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/csv")
                .returnResult(String.class)
                .getResponseBody()
                .collectList()
                .map(list -> String.join("\n", list))
                .block();

        String[] lines;
        if (response != null) {
            lines = response.trim().split("\\R");
            assertEquals(3, lines.length, "Expected 3 lines including header");
        }
    }

    @Test
    public void testInvalidJsonData() {
        String invalidJson = """
                [{"date":"invalid","productId":"8","currency":"EUR","price":450.20}]""";

        String response = webTestClient.post()
                .uri("/api/v1/trade")
                .header("Content-Type", "application/json")
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        if (response != null) {
            String[] lines = response.split("\n");
            assertEquals(1, lines.length);
            assertEquals("[]", lines[0]);
        }
    }

    @Test
    public void testInvalidXmlData() {
        String invalidXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <trades>
                    <trade><date>invalid</date><productId>10</productId><currency>USD</currency><price>550.40</price></trade>
                </trades>""";

        String response = webTestClient.post()
                .uri("/api/v1/trade")
                .header("Content-Type", "application/xml")
                .bodyValue(invalidXml)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/xml")
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();
        log.debug(response);
        if (response != null) {
            assertEquals(1, response.split("\n").length);
            assertEquals("<trades></trades>", response);
        }
    }

    @Test
    public void testInvalidCsvData() {
        String invalidCsv = """
                date,productId,currency,price
                invalid,2,USD,700.60""";

        String response = webTestClient.post()
                .uri("/api/v1/trade")
                .header("Content-Type", "text/csv")
                .bodyValue(invalidCsv)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("text/csv")
                .returnResult(String.class)
                .getResponseBody()
                .blockFirst();

        if (response != null) {
            String[] lines = response.split("\n");
            assertEquals(1, lines.length);
            assertEquals("date,productName,currency,price", lines[0]);
        }

    }
}