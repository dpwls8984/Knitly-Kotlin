package com.mysite.knitly.domain.order.event;

import com.mysite.knitly.domain.order.event.OrderCreatedEvent;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final StringRedisTemplate redisTemplate;
    private final RedisProductService redisProductService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        deleteProductCache(event.orderedProducts());
        incrementPopularityScore(event.orderedProducts());
    }

    private void deleteProductCache(List<Product> products) {
        if (products == null || products.isEmpty()) return;

        List<String> cacheKeys = products.stream()
                .map(product -> "product:detail:" + product.getProductId())
                .toList();

        if(!cacheKeys.isEmpty()) {
            try {
                redisTemplate.delete(cacheKeys);
                log.info("[Event] [Cache] 캐시 삭제(Invalidate) 완료 - keyCount={}", cacheKeys.size());
            } catch (Exception e) {
                log.error("[Event] [Cache] 캐시 삭제 실패 - keyCount={}, error={}",
                        cacheKeys.size(), e.getMessage(), e);
            }
        }
    }

    private void incrementPopularityScore(List<Product> products) {
        if (products == null || products.isEmpty()) return;

        try {
            for (Product product : products) {
                redisProductService.incrementPurchaseCount(product.getProductId());
            }
            log.info("[Event] [Redis] 인기도(ZSet) 점수 증가 완료 - productCount={}", products.size());
        } catch (Exception e) {
            log.error("[Event] [Redis] 인기도 점수 증가 실패 - error={}", e.getMessage(), e);
        }
    }
}