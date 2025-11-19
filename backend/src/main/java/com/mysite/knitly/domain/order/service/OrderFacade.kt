package com.mysite.knitly.domain.order.service

import com.mysite.knitly.domain.order.dto.OrderCreateRequest
import com.mysite.knitly.domain.order.dto.OrderCreateResponse
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.lock.RedisLockService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderFacade(
    private val redisLockService: RedisLockService,
    private val orderService: OrderService
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun createOrderWithLock(user: User, request: OrderCreateRequest): OrderCreateResponse {
        val lockKey = generateCompositeLockKey(request.productIds)
        log.info("[Order] [Facade] 주문 생성(락) 시작 - userId={}, lockKey={}", user.userId, lockKey)

        val startTime = System.currentTimeMillis()
        val waitTimeMillis: Long = 2000

        try {
            while (!redisLockService.tryLock(lockKey)) {
                if (System.currentTimeMillis() - startTime > waitTimeMillis) {
                    log.warn("[Order] [Facade] 락 획득 시간 초과 - key={}", lockKey)
                    throw RuntimeException("락 획득 시간 초과: $lockKey")
                }
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw RuntimeException("락 대기 중 인터럽트 발생", e)
                }
            }

            log.info("[Order] [Facade] 락 획득 성공 - key={}", lockKey)

            try {
                val createdOrder = orderService.createOrder(user, request.productIds)
                return OrderCreateResponse.from(createdOrder)
            } finally {
                redisLockService.unlock(lockKey)
                log.info("[Order] [Facade] 락 해제 완료 - key={}", lockKey)
            }

        } catch (e: Exception) {
            // RuntimeException이면서 메시지에 "락"이 포함된 경우는 이미 로그를 찍었거나 의도된 예외임
            if (e !is RuntimeException || e.message?.contains("락") != true) {
                log.error("[Order] [Facade] 주문 처리 중 예외 발생 - key={}", lockKey, e)
            }
            throw e
        }
    }

    private fun generateCompositeLockKey(productIds: List<Long>): String {
        return "order_lock:" + productIds.sorted().joinToString(":")
    }
}