package com.capybara.trade.enricher.service;

import com.capybara.trade.enricher.dto.EnrichedTradeDTO;
import com.capybara.trade.enricher.dto.TradeDTO;
import com.capybara.trade.enricher.exception.TradeValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TradeService {
    private static final Logger logger = LoggerFactory.getLogger(TradeService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern CSV_PATTERN = Pattern.compile(",");
    private final ProductMappingService productMappingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();

    public Mono<String> enrichTrades(String tradeData, String contentType) {
        return validateAndParseTrades(tradeData, contentType)
                .flatMap(this::enrichTrade)
                .collectList()
                .doOnSuccess(trades -> {
                    logger.debug("Finished enriching trades. Total enriched trades: {}", trades.size());
                    trades.forEach(trade -> logger.debug("Enriched Trade: {}", trade));
                })
                .map(trades -> TradeFormatter.formatTrades(trades, contentType));
    }

    private boolean isValidDate(String date) {
        try {
            LocalDate.parse(date, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            logger.warn("Invalid date format: {}", date);
            return false;
        }
    }

    private Mono<TradeDTO> createTradeDTO(String[] columns) {
        if (!isValidDate(columns[0].trim())) {
            return Mono.empty();
        }
        return Mono.just(new TradeDTO(
                columns[0].trim(),
                columns[1].trim(),
                columns[2].trim(),
                Double.parseDouble(columns[3].trim())
        ));
    }

    private Flux<TradeDTO> validateAndParseTrades(String tradeData, String contentType) {
        return Flux.defer(() -> {
            try {
                if ("text/csv".equalsIgnoreCase(contentType)) {
                    String[] lines = tradeData.split("\\r?\\n");
                    if (lines.length <= 1) {
                        return Flux.empty();
                    }
                    return Flux.fromStream(Arrays.stream(lines).skip(1))
                            .map(CSV_PATTERN::split)
                            .filter(columns -> columns.length == 4)
                            .flatMap(this::createTradeDTO);
                } else if ("application/json".equalsIgnoreCase(contentType)) {
                    TradeDTO[] trades = objectMapper.readValue(tradeData, TradeDTO[].class);
                    return Flux.fromArray(trades)
                            .filter(trade -> isValidDate(trade.getDate()));
                } else if ("application/xml".equalsIgnoreCase(contentType)) {
                    TradeDTO[] trades = xmlMapper.readValue(tradeData, TradeDTO[].class);
                    return Flux.fromArray(trades)
                            .filter(trade -> isValidDate(trade.getDate()));
                }
                return Flux.error(new TradeValidationException("Unsupported content type"));
            } catch (Exception e) {
                logger.error("Error parsing trades: {}", e.getMessage());
                return Flux.empty();
            }
        });
    }

    private Mono<EnrichedTradeDTO> enrichTrade(TradeDTO trade) {
        return productMappingService.getProductName(trade.getProductId())
                .defaultIfEmpty("Missing Product Name")
                .map(productName -> new EnrichedTradeDTO(
                        trade.getDate(),
                        trade.getProductId(),
                        trade.getCurrency(),
                        trade.getPrice(),
                        productName
                ));
    }
}