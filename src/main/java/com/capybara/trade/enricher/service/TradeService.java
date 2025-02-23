package com.capybara.trade.enricher.service;

import com.capybara.trade.enricher.dto.EnrichedTradeDTO;
import com.capybara.trade.enricher.dto.TradeDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        return parseTrades(tradeData, contentType)
                .flatMap(this::enrichTrade)
                .collectList()
                .doOnSuccess(trades -> logger.info("Finished enriching trades. Total enriched trades: {}", trades.size()))
                .map(trades -> TradeFormatter.formatTrades(trades, contentType));
    }

    private Mono<TradeDTO> createTradeDTO(String[] columns) {
        try {
            String date = columns[0].trim();
            LocalDate.parse(date, DATE_FORMATTER);
            return Mono.just(new TradeDTO(
                    date,
                    columns[1].trim(),
                    columns[2].trim(),
                    Double.parseDouble(columns[3].trim())
            ));
        } catch (Exception e) {
            logger.error("Invalid trade data: {} - {}", Arrays.toString(columns), e.getMessage());
            return Mono.empty();
        }
    }

    private Flux<TradeDTO> parseTrades(String tradeData, String contentType) {
        return Flux.defer(() -> {
            try {
                if ("text/csv".equalsIgnoreCase(contentType)) {
                    return Flux.fromStream(Arrays.stream(tradeData.split("\\r?\\n")).skip(1))
                            .map(CSV_PATTERN::split)
                            .filter(columns -> columns.length == 4)
                            .flatMap(this::createTradeDTO);
                } else if ("application/json".equalsIgnoreCase(contentType)) {
                    return Flux.fromArray(objectMapper.readValue(tradeData, TradeDTO[].class));
                } else if ("application/xml".equalsIgnoreCase(contentType)) {
                    return Flux.fromArray(xmlMapper.readValue(tradeData, TradeDTO[].class));
                }
            } catch (Exception e) {
                logger.error("Error parsing trades: {}", tradeData, e);
            }
            return Flux.empty();
        });
    }

    private Mono<EnrichedTradeDTO> enrichTrade(TradeDTO trade) {
        return productMappingService.getProductName(trade.getProductId())
                .map(productName -> {
                    if ("Missing Product Name".equals(productName)) {
                        logger.warn("Missing product mapping for productId: {}", trade.getProductId());
                    }
                    return new EnrichedTradeDTO(
                            trade.getDate(),
                            trade.getProductId(),
                            trade.getCurrency(),
                            trade.getPrice(),
                            productName
                    );
                });
    }

    public Mono<String> enrichStreamingTrades(Flux<DataBuffer> dataBufferFlux) {
        return StreamingTradeParser.parseCSV(dataBufferFlux)
                .windowTimeout(5000, Duration.ofSeconds(5))
                .flatMap(window -> window
                        .collectList()
                        .flatMap(trades -> Flux.fromIterable(trades)
                                .parallel(16)
                                .runOn(Schedulers.boundedElastic())
                                .flatMap(this::enrichTrade)
                                .sequential()
                                .collectList()))
                .collectList()
                .map(tradesList -> tradesList.stream().flatMap(List::stream)
                        .collect(Collectors.toList()))
                .doOnSuccess(trades -> logger.info("Finished streaming enrichment. Total enriched trades: {}", trades.size()))
                .map(trades -> TradeFormatter.formatTrades(trades, "text/csv"));
    }
}