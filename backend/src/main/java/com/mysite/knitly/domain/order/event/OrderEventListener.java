package com.mysite.knitly.domain.order.event;

import com.mysite.knitly.domain.order.event.OrderCreatedEvent;
import com.mysite.knitly.domain.payment.service.PaymentService;
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

    /**
     * 주문 생성 후 상품 상세 캐시 무효화
     * - 재고 정보가 변경되었으므로 캐시 삭제 필요
     * - 인기도는 결제 승인 시에만 증가해야하므로 메서드 삭제했음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        deleteProductCache(event.orderedProducts());
    }

    private void deleteProductCache(List<Product> products) {
        if (products == null || products.isEmpty()) {
            log.debug("[Event] [Cache] 캐시 삭제할 상품 없음");
            return;
        }

        List<String> cacheKeys = products.stream()
                .map(product -> "product:detail:" + product.getProductId())
                .toList();

        if (!cacheKeys.isEmpty()) {
            try {
                redisTemplate.delete(cacheKeys);
                log.info("[Event] [Cache] 상품 상세 캐시 삭제 완료 - keyCount={}", cacheKeys.size());
            } catch (Exception e) {
                log.error("[Event] [Cache] 캐시 삭제 실패 - keyCount={}, error={}",
                        cacheKeys.size(), e.getMessage(), e);
            }
        }
    }
}