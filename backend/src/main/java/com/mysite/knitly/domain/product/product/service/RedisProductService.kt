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
    private static final String POPULAR_LIST_CACHE_PREFIX = "product:list:popular:";
    private static final String HOME_POPULAR_TOP5_CACHE_KEY = "home:popular:top5";


    // 상품 구매시 인기도 증가
    public void incrementPurchaseCount(Long productId) {
        long startTime = System.currentTimeMillis();

        try {
            Double newScore = redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, productId.toString(), 1);
            long duration = System.currentTimeMillis() - startTime;

            log.info("[Redis] [Product] [IncrementScore] 인기도 증가 완료 - productId={}, newScore={}, duration={}ms",
                    productId, newScore, duration);

        } catch (Exception e) {
            log.error("[Redis] [Product] [IncrementScore] 인기도 증가 실패 - productId={}", productId, e);
        }
    }

    // 인기순 Top N 상품 조회
    public List<Long> getTopNPopularProducts(int n) {
        long startTime = System.currentTimeMillis();
        try{
            Set<String> top = redisTemplate.opsForZSet().reverseRange(POPULAR_KEY, 0, n - 1);
            if (top == null || top.isEmpty()) {
                long duration = System.currentTimeMillis() - startTime;
                log.warn("[Redis] [Product] [GetTopN] Redis에 데이터 없음 - requestedCount={}, duration={}ms", n, duration);
                return Collections.emptyList();
            }

            List result = top.stream().map(Long::valueOf).toList();
            long duration = System.currentTimeMillis() - startTime;

            log.info("[Redis] [Product] [GetTopN] Top N 조회 완료 - requestedCount={}, resultCount={}, duration={}ms",
                    n, result.size(), duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Redis] [Product] [GetTopN] Top N 조회 실패 - requestedCount={}, duration={}ms", n, duration, e);
            return Collections.emptyList();
        }
    }

    // DB의 purchaseCount를 Redis에 동기화
    public void syncFromDatabase(List<Product> products) {
        long startTime = System.currentTimeMillis();

        if (products == null || products.isEmpty()) {
            log.warn("[Redis] [Product] [Sync] 동기화할 상품 없음");
            return;
        }

        log.info("[Redis] [Product] [Sync] DB → Redis 동기화 시작 - productCount={}", products.size());

        try {
            int successCount = 0;
            int failCount = 0;

            for (Product product : products) {
                try {
                    redisTemplate.opsForZSet().add(
                            POPULAR_KEY,
                            product.getProductId().toString(),
                            product.getPurchaseCount()
                    );
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("[Redis] [Product] [Sync] 개별 상품 동기화 실패 - productId={}, purchaseCount={}",
                            product.getProductId(), product.getPurchaseCount(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            log.info("[Redis] [Product] [Sync] DB → Redis 동기화 완료 - totalCount={}, successCount={}, failCount={}, duration={}ms",
                    products.size(), successCount, failCount, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[Redis] [Product] [Sync] DB → Redis 동기화 실패 - productCount={}, duration={}ms",
                    products.size(), duration, e);
        }
    }

    // 인기순 목록 캐시 삭제
    public void evictPopularListCache() {
        long startTime = System.currentTimeMillis();
        try {
            Set<String> keys = redisTemplate.keys(POPULAR_LIST_CACHE_PREFIX + "*");
            int deletedCount = 0;
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                deletedCount += keys.size();
            }

            Boolean homeDeleted = redisTemplate.delete(HOME_POPULAR_TOP5_CACHE_KEY);
            if (Boolean.TRUE.equals(homeDeleted)) {
                deletedCount++;
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[Redis] [Product] [Cache] 인기순 관련 캐시 삭제 - deletedKeys={}, duration={}ms",
                    deletedCount, duration);

        } catch (Exception e) {
            log.error("[Redis] [Product] [Cache] 인기순 캐시 삭제 실패", e);
        }
    }

}
