package com.mysite.knitly.domain.order.event

import mu.KotlinLogging
import com.mysite.knitly.domain.product.product.entity.Product
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = KotlinLogging.logger {}

@Component
class OrderEventListener (
    private val redisTemplate: StringRedisTemplate
){
    /**
     * 주문 생성 후 상품 상세 캐시 무효화
     * - 재고 정보가 변경되었으므로 캐시 삭제 필요
     * - 인기도는 결제 승인 시에만 증가해야하므로 메서드 삭제했음
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCreatedEvent(event: OrderCreatedEvent) {
        deleteProductCache(event.orderedProducts)
    }

    private fun deleteProductCache(products: List<Product>?) {
        if (products.isNullOrEmpty()) {
            log.debug { "[Event] [Cache] 캐시 삭제할 상품 없음" }
            return
        }

        val cacheKeys = products
            .mapNotNull { it.productId }
            .map { "product:detail:$it" }

        if (!cacheKeys.isEmpty()) {
            try {
                redisTemplate.delete(cacheKeys)
                log.info { "[Event] [Cache] 상품 상세 캐시 삭제 완료 - keyCount=${cacheKeys.size}" }
            } catch (e: Exception) {
                log.error(e) { "[Event] [Cache] 캐시 삭제 실패 - keyCount=${cacheKeys.size}" }
            }
        }
    }
}