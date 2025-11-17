package com.mysite.knitly.domain.home.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.home.dto.HomeSummaryResponse
import com.mysite.knitly.domain.home.dto.LatestPostItem
import com.mysite.knitly.domain.home.dto.LatestReviewItem
import com.mysite.knitly.domain.home.repository.HomeQueryRepository
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository
import com.mysite.knitly.domain.product.product.dto.ProductListResponse
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.user.entity.User
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class HomeSectionService(
    private val redisProductService: RedisProductService,
    private val productRepository: ProductRepository,
    private val homeQueryRepository: HomeQueryRepository,
    private val productLikeRepository: ProductLikeRepository,
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(HomeSectionService::class.java)

    companion object {
        private const val HOME_POPULAR_TOP5_CACHE_KEY = "home:popular:top5"
    }

    // 인기 Top5 조회 - 홈 화면용
    fun getPopularTop5(user: User?): List<ProductListResponse> {
        val startTime = System.currentTimeMillis()
        val userId = user?.userId

        log.info("[Home] [Top5] 인기 Top5 조회 시작 - userId={}", userId)

        val cacheable = userId == null

        if (cacheable) {
            try {
                val cachedJson = stringRedisTemplate.opsForValue()
                    .get(HOME_POPULAR_TOP5_CACHE_KEY)

                if (cachedJson != null) {
                    val cachedResult = objectMapper.readValue(
                        cachedJson,
                        object : TypeReference<List<ProductListResponse>>() {}
                    )

                    val duration = System.currentTimeMillis() - startTime
                    log.info(
                        "[Home] [Top5] 캐시 히트 - key={}, resultCount={}, duration={}ms",
                        HOME_POPULAR_TOP5_CACHE_KEY, cachedResult.size, duration
                    )

                    return cachedResult
                }
            } catch (e: Exception) {
                log.error("[Home] [Top5] 캐시 조회 실패 - key={}", HOME_POPULAR_TOP5_CACHE_KEY, e)
            }
        }

        return try {
            val redisStartTime = System.currentTimeMillis()
            val topIds = redisProductService.getTopNPopularProducts(5)
            val redisDuration = System.currentTimeMillis() - redisStartTime

            log.debug(
                "[Home] [Top5] Redis 조회 완료 - count={}, redisDuration={}ms",
                topIds.size, redisDuration
            )

            val result = if (topIds.isEmpty()) {
                // Redis에 데이터 없으면 DB에서 직접 조회
                log.warn("[Home] [Top5] Redis 데이터 없음, DB에서 직접 조회")

                val dbStartTime = System.currentTimeMillis()
                val top5 = PageRequest.of(0, 5, Sort.by("purchaseCount").descending())
                val products = productRepository.findByIsDeletedFalse(top5).content

                val dbDuration = System.currentTimeMillis() - dbStartTime

                log.debug(
                    "[Home] [Top5] DB 직접 조회 완료 - count={}, dbDuration={}ms",
                    products.size, dbDuration
                )

                mapProductsToResponse(user, products)
            } else {
                // Redis에서 받은 ID로 DB에서 조회
                val dbStartTime = System.currentTimeMillis()
                val unorderedProducts = productRepository.findByProductIdInAndIsDeletedFalse(topIds)
                val dbDuration = System.currentTimeMillis() - dbStartTime

                log.debug(
                    "[Home] [Top5] DB 상품 정보 조회 완료 - requestedCount={}, foundCount={}, dbDuration={}ms",
                    topIds.size, unorderedProducts.size, dbDuration
                )

                // 찜 여부 확인
                val likeStartTime = System.currentTimeMillis()
                val likedProductIds = getLikedProductIds(user, unorderedProducts)
                val likeDuration = System.currentTimeMillis() - likeStartTime
                log.debug(
                    "[Home] [Top5] 좋아요 정보 조회 완료 - likedCount={}, likeDuration={}ms",
                    likedProductIds.size, likeDuration
                )

                // Redis 순서대로 정렬
                val productMap = unorderedProducts.associateBy { it.productId }

                topIds.mapNotNull { productMap[it] }
                    .map { product ->
                        ProductListResponse.from(
                            product,
                            likedProductIds.contains(product.productId)
                        )
                    }
            }

            val totalDuration = System.currentTimeMillis() - startTime
            log.info(
                "[Home] [Top5] 인기 Top5 조회 완료 - userId={}, resultCount={}, totalDuration={}ms",
                userId, result.size, totalDuration
            )

            if (cacheable) {
                try {
                    val jsonData = objectMapper.writeValueAsString(result)
                    stringRedisTemplate.opsForValue()
                        .set(HOME_POPULAR_TOP5_CACHE_KEY, jsonData, Duration.ofSeconds(60))

                    log.info(
                        "[Home] [Top5] 캐시 쓰기 완료 - key={}, ttl={}s",
                        HOME_POPULAR_TOP5_CACHE_KEY, 60
                    )
                } catch (e: Exception) {
                    log.error("[Home] [Top5] 캐시 쓰기 실패 - key={}", HOME_POPULAR_TOP5_CACHE_KEY, e)
                }
            }

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(
                "[Home] [Top5] 인기 Top5 조회 실패 - userId={}, duration={}ms",
                userId, duration, e
            )
            throw e
        }
    }

    // 최신 리뷰 N개
    fun getLatestReviews(limit: Int): List<LatestReviewItem> {
        val startTime = System.currentTimeMillis()

        log.debug("[Home] [LatestReviews] 최신 리뷰 조회 시작 - limit={}", limit)

        return try {
            val result = homeQueryRepository.findLatestReviews(limit)
            val duration = System.currentTimeMillis() - startTime
            log.info(
                "[Home] [LatestReviews] 최신 리뷰 조회 완료 - limit={}, resultCount={}, duration={}ms",
                limit, result.size, duration
            )

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(
                "[Home] [LatestReviews] 최신 리뷰 조회 실패 - limit={}, duration={}ms",
                limit, duration, e
            )
            throw e
        }
    }

    // 최신 커뮤니티 글 N개
    fun getLatestPosts(limit: Int): List<LatestPostItem> {
        val startTime = System.currentTimeMillis()

        log.debug("[Home] [LatestPosts] 최신 커뮤니티 글 조회 시작 - limit={}", limit)

        return try {
            val result = homeQueryRepository.findLatestPosts(limit)
            val duration = System.currentTimeMillis() - startTime
            log.info(
                "[Home] [LatestPosts] 최신 커뮤니티 글 조회 완료 - limit={}, resultCount={}, duration={}ms",
                limit, result.size, duration
            )

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(
                "[Home] [LatestPosts] 최신 커뮤니티 글 조회 실패 - limit={}, duration={}ms",
                limit, duration, e
            )
            throw e
        }
    }

    // 홈 (인기 5 + 최신 리뷰 3 + 최신 글 3)
    fun getHomeSummary(user: User?): HomeSummaryResponse {
        val startTime = System.currentTimeMillis()
        val userId = user?.userId

        log.info("[Home] [Summary] 홈 요약 정보 조회 시작 - userId={}", userId)

        return try {
            val popular = getPopularTop5(user)
            val reviews = getLatestReviews(3)
            val posts = getLatestPosts(3)
            val response = HomeSummaryResponse(popular, reviews, posts)

            val totalDuration = System.currentTimeMillis() - startTime
            log.info(
                "[Home] [Summary] 홈 요약 정보 조회 완료 - userId={}, popularCount={}, reviewCount={}, postCount={}, totalDuration={}ms",
                userId, popular.size, reviews.size, posts.size, totalDuration
            )

            response
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            log.error(
                "[Home] [Summary] 홈 요약 정보 조회 실패 - userId={}, duration={}ms",
                userId, duration, e
            )
            throw e
        }
    }

    private fun mapProductsToResponse(user: User?, products: List<Product>): List<ProductListResponse> {
        val likedProductIds = getLikedProductIds(user, products)
        return products.map { product ->
            ProductListResponse.from(
                product,
                likedProductIds.contains(product.productId)
            )
        }
    }

    private fun getLikedProductIds(user: User?, products: List<Product>): Set<Long> {
        if (user == null || products.isEmpty()) {
            return emptySet()
        }
        val productIds = products.mapNotNull { it.productId }
        return productLikeRepository.findLikedProductIdsByUserId(user.userId, productIds)
    }
}