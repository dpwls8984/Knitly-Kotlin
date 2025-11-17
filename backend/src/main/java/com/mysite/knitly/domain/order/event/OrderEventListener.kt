package com.mysite.knitly.domain.order.event

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.service.RedisProductService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
@Component
class OrderEventListener(
    private val redisTemplate: StringRedisTemplate,
    private val redisProductService: RedisProductService
) {

    companion object {
        private val log = LoggerFactory.getLogger(OrderEventListener::class.java)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCreatedEvent(event: OrderCreatedEvent) {
        deleteProductCache(event.orderedProducts)
        incrementPopularityScore(event.orderedProducts)
    }

    private fun deleteProductCache(products: List<Product>?) {
        if (products.isNullOrEmpty()) return

        val cacheKeys = products.map { "product:detail:" + it.productId }

        if (cacheKeys.isNotEmpty()) {
            try {
                redisTemplate.delete(cacheKeys)
                log.info("[Event] [Cache] 캐시 삭제(Invalidate) 완료 - keyCount={}", cacheKeys.size)
            } catch (e: Exception) {
                log.error(
                    "[Event] [Cache] 캐시 삭제 실패 - keyCount={}, error={}",
                    cacheKeys.size, e.message, e
                )
            }
        }
    }

    //TODO: incrementPopularityScore 개선
    private fun incrementPopularityScore(products: List<Product>?) {
        if (products.isNullOrEmpty()) return

        try {
            for (product in products) {
                // 10. 'redisProductService'가 Non-null이므로 '!!' 제거
                redisProductService.incrementPurchaseCount(product.productId)
            }
            log.info("[Event] [Redis] 인기도(ZSet) 점수 증가 완료 - productCount={}", products.size)
        } catch (e: Exception) {
            log.error("[Event] [Redis] 인기도 점수 증가 실패 - error={}", e.message, e)
        }
    }
}