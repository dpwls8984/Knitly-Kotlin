package com.mysite.knitly.domain.order.service;

import com.mysite.knitly.domain.order.dto.OrderCreateRequest;
import com.mysite.knitly.domain.order.dto.OrderCreateResponse;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.lock.RedisLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final RedisLockService redisLockService;
    private final OrderService orderService;

    public OrderCreateResponse createOrderWithLock(User user, OrderCreateRequest request) {
        // 락 키는 첫 번째 상품 ID를 기준으로 단순하게 생성 (혹은 모든 ID 조합)
        String lockKey = generateCompositeLockKey(request.productIds());
        log.info("[Order] [Facade] 주문 생성(락) 시작 - userId={}, lockKey={}", user.getUserId(), lockKey);

        long startTime = System.currentTimeMillis();
        long waitTimeMillis = 2000; // 최대 2초 대기

        try{
            log.debug("[Order] [Facade] 락 획득 시도 - key={}", lockKey);
            while (!redisLockService.tryLock(lockKey)) {
                // 현재 시간과 시작 시간을 비교하여 대기 시간을 초과했는지 확인
                if (System.currentTimeMillis() - startTime > waitTimeMillis) {
                    log.warn("[Order] [Facade] 락 획득 시간 초과 - key={}", lockKey);
                    throw new RuntimeException("락 획득 시간 초과: " + lockKey);
                }
                try {
                    log.trace("[Order] [Facade] 락 대기 중... - key={}", lockKey);
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("[Order] [Facade] 락 대기 중 인터럽트 발생 - key={}", lockKey, e);
                    throw new RuntimeException("락 대기 중 인터럽트 발생", e);
                }
            }
            log.info("[Order] [Facade] 락 획득 성공 - key={}", lockKey);

            try {
                Order createdOrder = orderService.createOrder(user, request.productIds());
                log.info("[Order] [Facade] 주문 생성(락) 완료 - orderId={}", createdOrder.getOrderId());
                return OrderCreateResponse.from(createdOrder);
            } catch (Exception e) {
                log.error("[Order] [Facade] 비즈니스 로직(OrderService) 실패 - key={}", lockKey, e);
                throw e;
            } finally {
                redisLockService.unlock(lockKey);
                log.info("[Order] [Facade] 락 해제 완료 - key={}", lockKey);
            }
        } catch (Exception e) {
            // 락 획득 실패 또는 기타 예외
            if (! (e instanceof RuntimeException && e.getMessage().contains("락"))) {
                log.error("[Order] [Facade] 락 획득 프로세스 실패 - key={}", lockKey, e);
            }
            throw e;
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