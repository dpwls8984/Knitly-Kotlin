package com.mysite.knitly.domain.order.service

import com.mysite.knitly.domain.order.dto.OrderCreateRequest
import com.mysite.knitly.domain.order.dto.OrderCreateResponse
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.lock.RedisLockService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class OrderFacade(
    private val redisLockService: RedisLockService,
    private val orderService: OrderService,
    private val productRepository: ProductRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun createOrder(user: User, request: OrderCreateRequest): OrderCreateResponse {
        val hasLimitedStock = productRepository.existsLimitedStockIn(request.productIds)

        return if (hasLimitedStock) {
            log.info("[Order] [Facade] 한정 재고 상품 포함 - 분산락 적용")
            createOrderWithLock(user, request)
        } else {
            log.info("[Order] [Facade] 일반 상품만 포함 - 락 없이 주문 생성")
            OrderCreateResponse.from(orderService.createOrder(user, request.productIds))
        }
    }

    private fun createOrderWithLock(user: User, request: OrderCreateRequest): OrderCreateResponse {
        val lockKey = generateCompositeLockKey(request.productIds)
        log.info("[Order] [Facade] 주문 생성(락) 시작 - userId={}, lockKey={}", user.userId, lockKey)

        val startTime = System.currentTimeMillis()
        val waitTimeMillis: Long = 3000
        var lockValue: String? = null

        try {
            // 스핀락: tryLock()이 UUID를 반환하면 성공, null이면 실패
            while (true) {
                lockValue = redisLockService.tryLock(lockKey)
                if (lockValue != null) break

                if (System.currentTimeMillis() - startTime > waitTimeMillis) {
                    log.warn("[Order] [Facade] 락 획득 시간 초과 - key={}", lockKey)
                    throw RuntimeException("락 획득 시간 초과: $lockKey")
                }
                try {
                    Thread.sleep(80 + Random.nextLong(40))
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
                redisLockService.unlock(lockKey, lockValue!!)
                log.info("[Order] [Facade] 락 해제 완료 - key={}", lockKey)
            }

        } catch (e: Exception) {
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
