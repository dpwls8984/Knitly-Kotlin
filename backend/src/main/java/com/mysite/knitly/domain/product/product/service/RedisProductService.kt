package com.mysite.knitly.domain.product.product.service

import com.mysite.knitly.domain.product.product.entity.Product
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class RedisProductService(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(RedisProductService::class.java)

    companion object {
        const val POPULAR_KEY = "product:popular"
        const val POPULAR_LIST_CACHE_PREFIX = "product:list:popular:"
        const val HOME_POPULAR_TOP5_CACHE_KEY = "home:popular:top5"
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
                emptyList()
            } else {
                val result = top.map { it.toLong() }
                val duration = System.currentTimeMillis() - startTime

                log.info(
                    "[Redis] [Product] [GetTopN] Top N 조회 완료 - requestedCount={}, resultCount={}, duration={}ms",
                    n, result.size, duration
                )

                result
            }
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
            // 기존 데이터 전체 삭제 후 재구축
            redisTemplate.delete(POPULAR_KEY)
            log.info("[Redis] [Product] [Sync] 기존 인기순 데이터 삭제 완료")

            val results = products.map { product ->
                try {
                    redisTemplate.opsForZSet().add(
                        POPULAR_KEY,
                        product.productId.toString(),
                        product.purchaseCount.toDouble()
                    )
                    true
                } catch (e: Exception) {
                    log.error(
                        "[Redis] [Product] [Sync] 개별 상품 동기화 실패 - productId={}, purchaseCount={}",
                        product.productId, product.purchaseCount, e
                    )
                    false
                }
            }

            val successCount = results.count { it }
            val failCount = results.count { !it }

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

    // 상품을 Redis에서 제거 (판매 중지 시)
    fun removeProduct(productId: Long) {
        val startTime = System.currentTimeMillis()

        try {
            val removed = redisTemplate.opsForZSet().remove(POPULAR_KEY, productId.toString())
            val duration = System.currentTimeMillis() - startTime

            log.info(
                "[Redis] [Product] [Remove] 상품 제거 완료 - productId={}, removed={}, duration={}ms",
                productId, removed, duration
            )

            // 캐시 무효화
            evictPopularListCache()
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(
                "[Redis] [Product] [Remove] 상품 제거 실패 - productId={}, duration={}ms",
                productId, duration, e
            )
        }
    }

    // 상품을 Redis에 추가 (판매 재개 시)
    fun addProduct(productId: Long, purchaseCount: Long) {
        val startTime = System.currentTimeMillis()

        try {
            val added = redisTemplate.opsForZSet().add(
                POPULAR_KEY,
                productId.toString(),
                purchaseCount.toDouble()
            )
            val duration = System.currentTimeMillis() - startTime

            log.info(
                "[Redis] [Product] [Add] 상품 추가 완료 - productId={}, purchaseCount={}, added={}, duration={}ms",
                productId, purchaseCount, added, duration
            )

            // 캐시 무효화
            evictPopularListCache()
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(
                "[Redis] [Product] [Add] 상품 추가 실패 - productId={}, purchaseCount={}, duration={}ms",
                productId, purchaseCount, duration, e
            )
        }
    }

    // 인기순 목록 캐시 삭제
    fun evictPopularListCache() {
        val startTime = System.currentTimeMillis()

        try {
            val keys = redisTemplate.keys("$POPULAR_LIST_CACHE_PREFIX*")

            val listCacheDeletedCount = if (!keys.isNullOrEmpty()) {
                redisTemplate.delete(keys)
                keys.size
            } else {
                0
            }

            val homeDeletedCount = when (redisTemplate.delete(HOME_POPULAR_TOP5_CACHE_KEY)) {
                true -> 1
                else -> 0
            }

            val deletedCount = listCacheDeletedCount + homeDeletedCount

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