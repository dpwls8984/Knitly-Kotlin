package com.mysite.knitly.domain.product.product.service

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.data.redis.core.ZSetOperations

@ExtendWith(MockitoExtension::class)
class RedisProductServiceTest {

    @Mock
    private lateinit var redisTemplate: StringRedisTemplate

    @Mock
    private lateinit var zSetOperations: ZSetOperations<String, String>

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    private lateinit var redisProductService: RedisProductService

    private lateinit var testUser: User
    private lateinit var testDesign: Design

    companion object {
        private const val POPULAR_KEY = "product:popular"
        private const val POPULAR_LIST_CACHE_PREFIX = "product:list:popular:"
        private const val HOME_POPULAR_TOP5_CACHE_KEY = "home:popular:top5"
    }

    @BeforeEach
    fun setUp() {
        redisProductService = RedisProductService(redisTemplate)

        testUser = User.builder()
            .userId(1L)
            .name("테스트유저")
            .email("test@test.com")
            .provider(Provider.GOOGLE)
            .build()

        testDesign = Design(
            designId = 1L,
            user = testUser,
            pdfUrl = "/files/design.pdf",
            designState = DesignState.ON_SALE,
            designType = null,
            designName = "테스트도안",
            gridData = "[]"
        )
    }

    @Test
    @DisplayName("상품 인기도 증가 - 정상 동작")
    fun incrementPurchaseCount_Success() {
        // given
        val productId = 1L
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        whenever(zSetOperations.incrementScore(POPULAR_KEY, "1", 1.0))
            .thenReturn(11.0)

        // when
        redisProductService.incrementPurchaseCount(productId)

        // then
        verify(zSetOperations).incrementScore(POPULAR_KEY, "1", 1.0)
    }

    @Test
    @DisplayName("상품 인기도 증가 - Redis 오류 발생해도 예외 안 던짐")
    fun incrementPurchaseCount_RedisError_NoException() {
        // given
        val productId = 1L
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        whenever(zSetOperations.incrementScore(POPULAR_KEY, "1", 1.0))
            .thenThrow(RuntimeException("Redis connection error"))

        // when & then - 예외가 발생하지 않아야 함
        redisProductService.incrementPurchaseCount(productId)

        verify(zSetOperations).incrementScore(POPULAR_KEY, "1", 1.0)
    }

    @Test
    @DisplayName("Top N 조회 - 데이터 있음")
    fun getTopNPopularProducts_WithData() {
        // given
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        val mockData = linkedSetOf("3", "1", "2")
        whenever(zSetOperations.reverseRange(POPULAR_KEY, 0, 2))
            .thenReturn(mockData)

        // when
        val result = redisProductService.getTopNPopularProducts(3)

        // then
        assertThat(result).hasSize(3)
        assertThat(result).containsExactly(3L, 1L, 2L)
        verify(zSetOperations).reverseRange(POPULAR_KEY, 0, 2)
    }

    @Test
    @DisplayName("Top N 조회 - 데이터 없음")
    fun getTopNPopularProducts_NoData() {
        // given
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        whenever(zSetOperations.reverseRange(POPULAR_KEY, 0, 4))
            .thenReturn(emptySet())

        // when
        val result = redisProductService.getTopNPopularProducts(5)

        // then
        assertThat(result).isEmpty()
        verify(zSetOperations).reverseRange(POPULAR_KEY, 0, 4)
    }

    @Test
    @DisplayName("Top N 조회 - null 반환")
    fun getTopNPopularProducts_NullResult() {
        // given
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        whenever(zSetOperations.reverseRange(POPULAR_KEY, 0, 4))
            .thenReturn(null)

        // when
        val result = redisProductService.getTopNPopularProducts(5)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("Top N 조회 - Redis 오류 발생")
    fun getTopNPopularProducts_RedisError() {
        // given
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        whenever(zSetOperations.reverseRange(POPULAR_KEY, 0, 4))
            .thenThrow(RuntimeException("Redis connection error"))

        // when
        val result = redisProductService.getTopNPopularProducts(5)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("DB → Redis 동기화 - 정상 동작")
    fun syncFromDatabase_Success() {
        // given
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        val products = listOf(
            createProduct(1L, 100),
            createProduct(2L, 200),
            createProduct(3L, 150)
        )

        whenever(zSetOperations.add(eq(POPULAR_KEY), eq("1"), eq(100.0)))
            .thenReturn(true)
        whenever(zSetOperations.add(eq(POPULAR_KEY), eq("2"), eq(200.0)))
            .thenReturn(true)
        whenever(zSetOperations.add(eq(POPULAR_KEY), eq("3"), eq(150.0)))
            .thenReturn(true)

        // when
        redisProductService.syncFromDatabase(products)

        // then
        verify(zSetOperations, times(3)).add(eq(POPULAR_KEY), any<String>(), any<Double>())
    }

    @Test
    @DisplayName("DB → Redis 동기화 - 빈 리스트")
    fun syncFromDatabase_EmptyList() {
        // when
        redisProductService.syncFromDatabase(emptyList())

        // then - redisTemplate이 호출되지 않아야 함
        verifyNoInteractions(redisTemplate)
    }

    @Test
    @DisplayName("DB → Redis 동기화 - null 리스트")
    fun syncFromDatabase_NullList() {
        // when
        redisProductService.syncFromDatabase(null)

        // then - redisTemplate이 호출되지 않아야 함
        verifyNoInteractions(redisTemplate)
    }

    @Test
    @DisplayName("DB → Redis 동기화 - 일부 실패")
    fun syncFromDatabase_PartialFailure() {
        // given
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        val products = listOf(
            createProduct(1L, 100),
            createProduct(2L, 200),
            createProduct(3L, 150)
        )

        whenever(zSetOperations.add(eq(POPULAR_KEY), eq("1"), eq(100.0)))
            .thenReturn(true)
        whenever(zSetOperations.add(eq(POPULAR_KEY), eq("2"), eq(200.0)))
            .thenThrow(RuntimeException("Redis error"))
        whenever(zSetOperations.add(eq(POPULAR_KEY), eq("3"), eq(150.0)))
            .thenReturn(true)

        // when
        redisProductService.syncFromDatabase(products)

        // then
        verify(zSetOperations, times(3)).add(eq(POPULAR_KEY), any<String>(), any<Double>())
    }

    @Test
    @DisplayName("DB → Redis 동기화 - purchaseCount가 0인 상품도 동기화")
    fun syncFromDatabase_ZeroPurchaseCount() {
        // given
        whenever(redisTemplate.opsForZSet()).thenReturn(zSetOperations)
        val products = listOf(
            createProduct(1L, 0),
            createProduct(2L, 100)
        )

        whenever(zSetOperations.add(eq(POPULAR_KEY), any<String>(), any<Double>()))
            .thenReturn(true)

        // when
        redisProductService.syncFromDatabase(products)

        // then
        verify(zSetOperations).add(POPULAR_KEY, "1", 0.0)
        verify(zSetOperations).add(POPULAR_KEY, "2", 100.0)
    }

    @Test
    @DisplayName("인기순 목록 캐시 삭제 - 정상 동작")
    fun evictPopularListCache_Success() {
        // given
        val cacheKeys = setOf(
            "${POPULAR_LIST_CACHE_PREFIX}page:0",
            "${POPULAR_LIST_CACHE_PREFIX}page:1",
            "${POPULAR_LIST_CACHE_PREFIX}page:2"
        )

        whenever(redisTemplate.keys("$POPULAR_LIST_CACHE_PREFIX*"))
            .thenReturn(cacheKeys)
        whenever(redisTemplate.delete(cacheKeys))
            .thenReturn(3L)
        whenever(redisTemplate.delete(HOME_POPULAR_TOP5_CACHE_KEY))
            .thenReturn(true)

        // when
        redisProductService.evictPopularListCache()

        // then
        verify(redisTemplate).keys("$POPULAR_LIST_CACHE_PREFIX*")
        verify(redisTemplate).delete(cacheKeys)
        verify(redisTemplate).delete(HOME_POPULAR_TOP5_CACHE_KEY)
    }

    @Test
    @DisplayName("인기순 목록 캐시 삭제 - 캐시 없음")
    fun evictPopularListCache_NoCacheKeys() {
        // given
        whenever(redisTemplate.keys("$POPULAR_LIST_CACHE_PREFIX*"))
            .thenReturn(emptySet())
        whenever(redisTemplate.delete(HOME_POPULAR_TOP5_CACHE_KEY))
            .thenReturn(false)

        // when
        redisProductService.evictPopularListCache()

        // then
        verify(redisTemplate).keys("$POPULAR_LIST_CACHE_PREFIX*")
        verify(redisTemplate, never()).delete(any<Set<String>>())
        verify(redisTemplate).delete(HOME_POPULAR_TOP5_CACHE_KEY)
    }

    @Test
    @DisplayName("인기순 목록 캐시 삭제 - Redis 오류 발생")
    fun evictPopularListCache_RedisError() {
        // given
        whenever(redisTemplate.keys("$POPULAR_LIST_CACHE_PREFIX*"))
            .thenThrow(RuntimeException("Redis connection error"))

        // when & then - 예외가 발생하지 않아야 함
        redisProductService.evictPopularListCache()

        verify(redisTemplate).keys("$POPULAR_LIST_CACHE_PREFIX*")
    }

    private fun createProduct(productId: Long, purchaseCount: Int): Product {
        return Product(
            productId = productId,
            title = "상품$productId",
            description = "설명",
            productCategory = ProductCategory.TOP,
            sizeInfo = "M",
            price = 10000.0,
            purchaseCount = purchaseCount,
            isDeleted = false,
            user = testUser,
            design = testDesign,
        )
    }
}