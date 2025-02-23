package com.capybara.trade.enricher.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeDTO {
    private String date;
    private String productId;
    private String currency;
    private Double price;
}