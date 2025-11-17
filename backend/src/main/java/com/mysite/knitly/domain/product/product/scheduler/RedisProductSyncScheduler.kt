package com.mysite.knitly.domain.product.product.scheduler

import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.product.service.RedisProductService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Page
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

    // 애플리케이션 시작 시 DB → Redis 초기 로딩
    @EventListener(ApplicationReadyEvent::class)
    fun initializedRedisData() {
        log.info("[Scheduler] [Redis] 애플리케이션 시작 - Redis 초기 데이터 로딩 시작")

        val startTime = System.currentTimeMillis()

        try {
            // 전체 상품 조회 (페이징으로 모든 데이터 가져오기)
            val allProducts = mutableListOf<Product>()
            var pageNumber = 0
            val pageSize = 1000
            var page: Page<Product>

            do {
                val pageable = PageRequest.of(pageNumber, pageSize)
                page = productRepository.findByIsDeletedFalse(pageable)
                allProducts.addAll(page.content)
                pageNumber++
            } while (page.hasNext())

            if (allProducts.isEmpty()) {
                log.warn("[Scheduler] [Redis] 초기 로딩할 상품이 없습니다")
                return
            }

            redisProductService.syncFromDatabase(allProducts)

            val duration = System.currentTimeMillis() - startTime
            log.info(
                "[Scheduler] [Redis] Redis 초기 데이터 로딩 완료 - productCount={}, duration={}ms",
                allProducts.size, duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error("[Scheduler] [Redis] Redis 초기 데이터 로딩 실패 - duration={}ms", duration, e)
        }
    }

    // 매일 새벽 3시에 DB → Redis 전체 동기화
    @Scheduled(cron = "0 0 3 * * *")
    fun syncPurchaseCountFromDB() {
        log.info("[Scheduler] [Redis] 정기 동기화 시작 - DB → Redis")

        val startTime = System.currentTimeMillis()

        try {
            // 전체 상품 조회 (페이징으로 모든 데이터 가져오기)
            val allProducts = mutableListOf<Product>()
            var pageNumber = 0
            val pageSize = 1000
            var page: Page<Product>

            do {
                val pageable = PageRequest.of(pageNumber, pageSize)
                page = productRepository.findByIsDeletedFalse(pageable)
                allProducts.addAll(page.content)
                pageNumber++
            } while (page.hasNext())

            if (allProducts.isEmpty()) {
                log.warn("[Scheduler] [Redis] 동기화할 상품이 없습니다")
                return
            }

            redisProductService.syncFromDatabase(allProducts)

            val duration = System.currentTimeMillis() - startTime
            log.info(
                "[Scheduler] [Redis] 정기 동기화 완료 - productCount={}, duration={}ms",
                allProducts.size, duration
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error("[Scheduler] [Redis] 정기 동기화 실패 - duration={}ms", duration, e)
        }
    }
}
