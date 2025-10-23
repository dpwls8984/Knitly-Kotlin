package com.mysite.knitly.domain.order.service;

import com.mysite.knitly.domain.order.dto.OrderCreateRequest;
import com.mysite.knitly.domain.order.dto.OrderCreateResponse;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.lock.RedisLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final RedisLockService redisLockService;
    private final OrderService orderService;

    public OrderCreateResponse createOrderWithLock(User user, OrderCreateRequest request) {
        // 락 키는 첫 번째 상품 ID를 기준으로 단순하게 생성 (혹은 모든 ID 조합)
        String lockKey = generateCompositeLockKey(request.productIds());
        while (!redisLockService.tryLock(lockKey)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("락 대기 중 인터럽트 발생", e);
            }
        }

        try {
            Order createdOrder = orderService.createOrder(user, request.productIds());
            return OrderCreateResponse.from(createdOrder);
        } finally {
            redisLockService.unlock(lockKey);
        }
    }

    /**
     * 상품 ID 리스트를 정렬하여 복합 락 키를 생성합니다.
     * 정렬하는 이유: [A, B]와 [B, A]에 대해 동일한 락 키를 보장하여 데드락을 방지합니다.
     */
    private String generateCompositeLockKey(List<Long> productIds) {
        String sortedIds = productIds.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
        return "order_lock:" + sortedIds;
    }
}