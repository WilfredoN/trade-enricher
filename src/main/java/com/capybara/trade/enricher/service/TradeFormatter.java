package com.capybara.trade.enricher.service;

import com.capybara.trade.enricher.dto.EnrichedTradeDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TradeFormatter {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final XmlMapper xmlMapper = new XmlMapper();

    @SneakyThrows
    public static String formatTrades(List<EnrichedTradeDTO> trades, String contentType) {
        if ("text/csv".equalsIgnoreCase(contentType)) {
            return "date,productName,currency,price\n" + trades.stream()
                    .map(trade -> String.join(",", trade.getDate(), trade.getProductName(), trade.getCurrency(), String.valueOf(trade.getPrice())))
                    .collect(Collectors.joining("\n"));
        } else if ("application/json".equalsIgnoreCase(contentType) || "application/xml".equalsIgnoreCase(contentType)) {
            List<Map<String, Object>> formattedTrades = trades.stream()
                    .map(trade -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("date", trade.getDate());
                        map.put("productName", trade.getProductName());
                        map.put("currency", trade.getCurrency());
                        map.put("price", trade.getPrice());
                        return map;
                    })
                    .collect(Collectors.toList());
            if ("application/json".equalsIgnoreCase(contentType)) {
                return objectMapper.writeValueAsString(formattedTrades);
            } else {
                return xmlMapper.writeValueAsString(formattedTrades);
            }
        }
        return "";
    }
}