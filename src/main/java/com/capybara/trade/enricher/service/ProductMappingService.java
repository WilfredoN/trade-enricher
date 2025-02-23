package com.capybara.trade.enricher.service;

import com.capybara.trade.enricher.model.Product;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ProductMappingService {
    private static final Logger logger = LoggerFactory.getLogger(ProductMappingService.class);
    private final ReactiveRedisTemplate<String, Product> reactiveRedisTemplate;
    private final Map<String, String> productCache = new ConcurrentHashMap<>();

    public void reloadCache() {
        reactiveRedisTemplate.keys("product:*")
                .buffer(1000)
                .flatMap(keys -> reactiveRedisTemplate.opsForValue().multiGet(keys)
                        .flatMapIterable(products -> products)
                        .filter(Objects::nonNull))
                .doOnNext(product -> {
                    productCache.put(product.getProductId(), product.getProductName());
                    logger.debug("Loaded product {} into cache", product.getProductId());
                })
                .doOnError(e -> logger.error("Error reloading products from Redis", e))
                .subscribe();
    }

    public Mono<String> getProductName(String productId) {
        String productName = productCache.get(productId);
        if (productName != null) {
            return Mono.just(productName);
        }
        return reactiveRedisTemplate.opsForValue().get("product:" + productId)
                .map(product -> {
                    if (product != null) {
                        productCache.put(product.getProductId(), product.getProductName());
                        return product.getProductName();
                    }
                    return "Missing Product Name";
                })
                .defaultIfEmpty("Missing Product Name");
    }
}
