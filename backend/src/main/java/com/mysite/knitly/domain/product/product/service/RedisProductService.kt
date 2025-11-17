package com.mysite.knitly.domain.product.product.service

import com.mysite.knitly.domain.product.product.entity.Product
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.lang.Boolean
import kotlin.Exception
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.toString

@Service
class RedisProductService(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(RedisProductService::class.java)

    companion object {
        private const val POPULAR_KEY = "product:popular"
        private const val POPULAR_LIST_CACHE_PREFIX = "product:list:popular:"
        private const val HOME_POPULAR_TOP5_CACHE_KEY = "home:popular:top5"
    }

    // 상품 구매시 인기도 증가
    fun incrementPurchaseCount(productId: Long) {
        val startTime = System.currentTimeMillis()

        try {
            val newScore = redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, productId.toString(), 1.0)
            val duration = System.currentTimeMillis() - startTime

            log.info(
                "[Redis] [Product] [IncrementScore] 인기도 증가 완료 - productId={}, newScore={}, duration={}ms",
                productId, newScore, duration
            )
        } catch (e: Exception) {
            log.error("[Redis] [Product] [IncrementScore] 인기도 증가 실패 - productId={}", productId, e)
        }
    }

    // 인기순 Top N 상품 조회
    fun getTopNPopularProducts(n: Int): List<Long> {
        val startTime = System.currentTimeMillis()
        return try {
            val top = redisTemplate.opsForZSet().reverseRange(POPULAR_KEY, 0, (n - 1).toLong())

            if (top.isNullOrEmpty()) {
                val duration = System.currentTimeMillis() - startTime
                log.warn("[Redis] [Product] [GetTopN] Redis에 데이터 없음 - requestedCount={}, duration={}ms", n, duration)
                return emptyList()
            }

            val result = top.map { it.toLong() }
            val duration = System.currentTimeMillis() - startTime

            log.info(
                "[Redis] [Product] [GetTopN] Top N 조회 완료 - requestedCount={}, resultCount={}, duration={}ms",
                n, result.size, duration
            )

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error("[Redis] [Product] [GetTopN] Top N 조회 실패 - requestedCount={}, duration={}ms", n, duration, e)
            emptyList()
        }
    }

    // DB의 purchaseCount를 Redis에 동기화
    fun syncFromDatabase(products: List<Product>?) {
        val startTime = System.currentTimeMillis()

        if (products.isNullOrEmpty()) {
            log.warn("[Redis] [Product] [Sync] 동기화할 상품 없음")
            return
        }

        log.info("[Redis] [Product] [Sync] DB → Redis 동기화 시작 - productCount={}", products.size)

        try {
            var successCount = 0
            var failCount = 0

            products.forEach { product ->
                try {
                    redisTemplate.opsForZSet().add(
                        POPULAR_KEY,
                        product.productId.toString(),
                        product.purchaseCount.toDouble()
                    )
                    successCount++
                } catch (e: Exception) {
                    failCount++
                    log.error(
                        "[Redis] [Product] [Sync] 개별 상품 동기화 실패 - productId={}, purchaseCount={}",
                        product.productId, product.purchaseCount, e
                    )
                }
            }

            val duration = System.currentTimeMillis() - startTime

            log.info(
                "[Redis] [Product] [Sync] DB → Redis 동기화 완료 - totalCount={}, successCount={}, failCount={}, duration={}ms",
                products.size, successCount, failCount, duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(
                "[Redis] [Product] [Sync] DB → Redis 동기화 실패 - productCount={}, duration={}ms",
                products.size, duration, e
            )
        }
    }

    // 인기순 목록 캐시 삭제
    fun evictPopularListCache() {
        val startTime = System.currentTimeMillis()
        try {
            val keys = redisTemplate.keys("$POPULAR_LIST_CACHE_PREFIX*")
            var deletedCount = 0

            if (!keys.isNullOrEmpty()) {
                redisTemplate.delete(keys)
                deletedCount += keys.size
            }

            val homeDeleted = redisTemplate.delete(HOME_POPULAR_TOP5_CACHE_KEY)
            if (homeDeleted == true) {
                deletedCount++
            }

            val duration = System.currentTimeMillis() - startTime
            log.info(
                "[Redis] [Product] [Cache] 인기순 관련 캐시 삭제 - deletedKeys={}, duration={}ms",
                deletedCount, duration
            )
        } catch (e: Exception) {
            log.error("[Redis] [Product] [Cache] 인기순 캐시 삭제 실패", e)
        }
    }
}