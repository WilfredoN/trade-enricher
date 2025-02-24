package com.capybara.trade.enricher.service;

import com.capybara.trade.enricher.dto.EnrichedTradeDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class TradeFormatter {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final XmlMapper xmlMapper = new XmlMapper();
    private static final String CSV_HEADER = "date,productName,currency,price";

    @SneakyThrows
    public static String formatTrades(List<EnrichedTradeDTO> trades, String contentType) {
        if (trades == null || trades.isEmpty()) {
            return switch (contentType.toLowerCase()) {
                case "text/csv" -> CSV_HEADER;
                case "application/json" -> "[]";
                case "application/xml" -> "<trades></trades>";
                default -> "";
            };
        }

        return switch (contentType.toLowerCase()) {
            case "text/csv" -> formatCsv(trades);
            case "application/json" -> objectMapper.writeValueAsString(trades);
            case "application/xml" -> formatXml(trades);
            default -> "";
        };
    }

    private static String formatCsv(List<EnrichedTradeDTO> trades) {
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER).append('\n');

        for (EnrichedTradeDTO trade : trades) {
            csv.append(String.format("%s,%s,%s,%.2f%n",
                    trade.getDate(),
                    trade.getProductName(),
                    trade.getCurrency(),
                    trade.getPrice()));
        }
        log.debug("Formatted CSV: {}", csv);

        return csv.toString();
    }

    @SneakyThrows
    private static String formatXml(List<EnrichedTradeDTO> trades) {
        return xmlMapper.writeValueAsString(trades)
                .replace("[", "<trades>")
                .replace("]", "</trades>");
    }
}