package com.capybara.trade.enricher.service;

import com.capybara.trade.enricher.model.Product;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductLoaderService {
    private static final Logger logger = LoggerFactory.getLogger(ProductLoaderService.class);
    private static final int BATCH_SIZE = 1000;
    private final ReactiveRedisTemplate<String, Product> reactiveRedisTemplate;
    private final ProductMappingService productMappingService;

    @Value("${product.loader.file}")
    private String productFile;

    @PostConstruct
    public void loadProductsFromFile() {
        logger.info("Starting product load from file: {}", productFile);
        Instant start = Instant.now();
        AtomicInteger totalProducts = new AtomicInteger(0);
        AtomicInteger batchCount = new AtomicInteger(0);
        AtomicInteger invalidLines = new AtomicInteger(0);
        ClassPathResource resource = new ClassPathResource(productFile);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            reader.readLine();
            List<String> batch = new ArrayList<>(BATCH_SIZE);
            String line;
            while ((line = reader.readLine()) != null) {
                batch.add(line);
                if (batch.size() >= BATCH_SIZE) {
                    int invalid = processProductLines(batch, batchCount.incrementAndGet());
                    invalidLines.addAndGet(invalid);
                    totalProducts.addAndGet(batch.size() - invalid);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                int invalid = processProductLines(batch, batchCount.incrementAndGet());
                invalidLines.addAndGet(invalid);
                totalProducts.addAndGet(batch.size() - invalid);
            }
            Duration duration = Duration.between(start, Instant.now());
            logFinalStats(totalProducts.get(), invalidLines.get(), batchCount.get(), duration);
        } catch (IOException e) {
            logger.error("Failed to read product file: {}", productFile, e);
        }
        logger.info("Triggering product mapping cache reload");
        productMappingService.reloadCache();
    }

    private int processProductLines(List<String> lines, int batchNumber) {
        Instant batchStart = Instant.now();
        AtomicInteger invalidLines = new AtomicInteger(0);
        Map<String, Product> products = lines.stream()
                .map(line -> line.split(","))
                .filter(tokens -> {
                    boolean valid = tokens.length == 2;
                    if (!valid) invalidLines.incrementAndGet();
                    return valid;
                })
                .collect(Collectors.toMap(
                        tokens -> "product:" + tokens[0].trim(),
                        tokens -> new Product(tokens[0].trim(), tokens[1].trim())
                ));
        reactiveRedisTemplate.opsForValue()
                .multiSet(products)
                .doOnError(e -> logger.error("Failed to save batch #{} to Redis", batchNumber, e))
                .subscribe(result -> {
                    Duration batchDuration = Duration.between(batchStart, Instant.now());
                    logger.info("Batch #{}: Processed {} products ({} valid, {} invalid) in {} ms",
                            batchNumber, lines.size(), products.size(), invalidLines.get(), batchDuration.toMillis());
                });
        return invalidLines.get();
    }

    private void logFinalStats(int totalProducts, int invalidLines, int batchCount, Duration duration) {
        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        logger.info("Product loading completed:");
        logger.info("Total products processed: {}", totalProducts);
        logger.info("Invalid lines skipped: {}", invalidLines);
        logger.info("Number of batches: {}", batchCount);
        logger.info("Total processing time: {} seconds", duration.getSeconds());
        logger.info("Memory used: {} MB", memoryUsed / (1024 * 1024));
        logger.info("Average processing time per product: {} ms", totalProducts > 0 ? duration.toMillis() / totalProducts : 0);
    }
}
