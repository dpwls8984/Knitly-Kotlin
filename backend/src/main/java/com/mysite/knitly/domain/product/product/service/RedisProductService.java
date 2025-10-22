package com.mysite.knitly.domain.product.product.service;

import com.mysite.knitly.domain.product.product.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisProductService {

    private final StringRedisTemplate redisTemplate;
    private static final String POPULAR_KEY = "product:popular";


    // 상품 구매시 인기도 증가
    public void incrementPurchaseCount(Long productId) {
        redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, productId.toString(), 1);
        log.debug("Redis 인기도 증가: productId={}", productId);
    }

    // 인기순 Top N 상품 조회
    public List<Long> getTopNPopularProducts(int n) {
        Set<String> top = redisTemplate.opsForZSet().reverseRange(POPULAR_KEY, 0, n - 1);
        if (top == null || top.isEmpty()) return Collections.emptyList();
        return top.stream().map(Long::valueOf).toList();
    }

    // DB의 purchaseCount를 Redis에 동기화
    public void syncFromDatabase(List<Product> products) {
        products.forEach(product -> {
            redisTemplate.opsForZSet().add(
                    POPULAR_KEY,
                    product.getProductId().toString(),
                    product.getPurchaseCount()
            );
        });
        log.info("Redis 동기화 완료: {} 개 상품", products.size());
    }
}
