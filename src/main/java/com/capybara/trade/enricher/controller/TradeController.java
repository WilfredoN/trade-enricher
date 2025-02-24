package com.capybara.trade.enricher.controller;

import com.capybara.trade.enricher.exception.TradeValidationException;
import com.capybara.trade.enricher.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TradeController {
    private static final Logger logger = LoggerFactory.getLogger(TradeController.class);
    private final TradeService tradeService;

    @PostMapping(value = "/trade")
    public Mono<ResponseEntity<String>> handleTrade(
            @RequestBody String trade,
            @RequestHeader("Content-Type") String contentType) {
        if (!isValidContentType(contentType)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .header("Content-Type", "text/plain")
                    .body("Unsupported Content-Type"));
        }

        Mono<String> result = tradeService.enrichTrades(trade, contentType)
                .doOnError(TradeValidationException.class, e -> logger.error("Trade validation error: {}", e.getMessage()));
        logger.debug("Trade enrichment result: {}", result);

        return result
                .map(res -> ResponseEntity.ok()
                        .header("Content-Type", contentType)
                        .body(res));
    }

    private boolean isValidContentType(String contentType) {
        return "text/csv".equalsIgnoreCase(contentType)
                || "application/json".equalsIgnoreCase(contentType)
                || "application/xml".equalsIgnoreCase(contentType);
    }

}