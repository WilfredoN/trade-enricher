package com.capybara.trade.enricher.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class Trade {
    private LocalDate date;
    private String productId;
    private String currency;
    private Double price;
}