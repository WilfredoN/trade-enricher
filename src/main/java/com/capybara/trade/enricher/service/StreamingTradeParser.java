package com.capybara.trade.enricher.service;

import com.capybara.trade.enricher.dto.TradeDTO;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class StreamingTradeParser {
    public static Flux<TradeDTO> parseCSV(Flux<DataBuffer> dataBufferFlux) {
        AtomicReference<String> remainder = new AtomicReference<>("");

        return dataBufferFlux
                .publishOn(Schedulers.boundedElastic())
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return remainder.get() + new String(bytes, StandardCharsets.UTF_8);
                })
                .map(content -> {
                    int lastNewLine = content.lastIndexOf('\n');
                    String processable = content.substring(0, lastNewLine);
                    remainder.set(content.substring(lastNewLine + 1));
                    return processable;
                })
                .flatMap(chunk -> Flux.fromArray(chunk.split("\n")))
                .skip(1)
                .map(line -> line.split(","))
                .filter(columns -> columns.length == 4)
                .map(columns -> new TradeDTO(
                        columns[0].trim(),
                        columns[1].trim(),
                        columns[2].trim(),
                        Double.parseDouble(columns[3].trim())
                ));
    }
}