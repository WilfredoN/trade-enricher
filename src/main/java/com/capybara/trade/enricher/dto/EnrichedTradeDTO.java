package com.capybara.trade.enricher.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JacksonXmlRootElement(localName = "enrichedTrade")
public class EnrichedTradeDTO extends TradeDTO {
    private String productName;

    public EnrichedTradeDTO(String date, String productId, String currency, Double price, String productName) {
        super(date, productId, currency, price);
        this.productName = productName;
    }
}