package com.mysite.knitly.domain.product.product.scheduler

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.product.service.RedisProductService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Redis와 DB 간 상품 인기도 데이터 동기화 스케줄러
 *
 * 1. 애플리케이션 시작 시: DB → Redis 초기 로딩
 * 2. 매일 새벽 3시: DB → Redis 전체 동기화
 */
@Component
class RedisProductSyncScheduler(
    private val productRepository: ProductRepository,
    private val redisProductService: RedisProductService
) {
    private val log = LoggerFactory.getLogger(RedisProductSyncScheduler::class.java)

    companion object {
        private const val PAGE_SIZE = 1000
    }

    // 애플리케이션 시작 시 DB → Redis 초기 로딩
    @EventListener(ApplicationReadyEvent::class)
    fun initializedRedisData() {
        syncAllProducts("애플리케이션 시작 - Redis 초기 데이터 로딩")
    }

    // 매일 새벽 3시에 DB → Redis 전체 동기화
    @Scheduled(cron = "0 0 3 * * *")
    fun syncPurchaseCountFromDB() {
        syncAllProducts("정기 동기화 - DB → Redis")
    }

    /**
     * 모든 상품을 DB에서 조회하여 Redis에 동기화
     */
    private fun syncAllProducts(taskName: String) {
        log.info("[Scheduler] [Redis] {} 시작", taskName)
        val startTime = System.currentTimeMillis()

        try {
            val allProducts = fetchAllProducts()

            if (allProducts.isEmpty()) {
                log.warn("[Scheduler] [Redis] {}할 상품이 없습니다", taskName)
                return
            }

            redisProductService.syncFromDatabase(allProducts)

            val duration = System.currentTimeMillis() - startTime
            log.info(
                "[Scheduler] [Redis] {} 완료 - productCount={}, duration={}ms",
                taskName, allProducts.size, duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error("[Scheduler] [Redis] {} 실패 - duration={}ms", taskName, duration, e)
        }
    }

    /**
     * 페이징을 통해 모든 상품 조회 (꼬리 재귀)
     */
    private fun fetchAllProducts(): List<Product> =
        fetchAllProductsRecursive()

    private tailrec fun fetchAllProductsRecursive(
        pageNumber: Int = 0,
        accumulated: List<Product> = emptyList()
    ): List<Product> {
        val pageable = PageRequest.of(pageNumber, PAGE_SIZE)
        val page = productRepository.findByIsDeletedFalse(pageable)

        val newAccumulated = accumulated + page.content

        return if (page.hasNext()) {
            fetchAllProductsRecursive(pageNumber + 1, newAccumulated)
        } else {
            newAccumulated
        }
    }
}