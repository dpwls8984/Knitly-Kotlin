package com.mysite.knitly.domain.home.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.home.dto.HomeSummaryResponse
import com.mysite.knitly.domain.home.dto.LatestPostItem
import com.mysite.knitly.domain.home.dto.LatestReviewItem
import com.mysite.knitly.domain.home.repository.HomeQueryRepository
import com.mysite.knitly.domain.product.like.repository.ProductLikeRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomeSectionServiceTest {

    @Mock
    private lateinit var redisProductService: RedisProductService

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var homeQueryRepository: HomeQueryRepository

    @Mock
    private lateinit var productLikeRepository: ProductLikeRepository

    @Mock
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    @InjectMocks
    private lateinit var homeSectionService: HomeSectionService

    private lateinit var product1: Product // id=1
    private lateinit var product2: Product // id=2
    private lateinit var product3: Product // id=3

    @BeforeEach
    fun setUp() {
        product1 = createProduct(
            id = 1L,
            title = "상의 패턴 1",
            category = ProductCategory.TOP,
            price = 10_000.0,
            purchaseCount = 100,
            likeCount = 50,
            isDeleted = false,
        )

        product2 = createProduct(
            id = 2L,
            title = "무료 패턴",
            category = ProductCategory.BOTTOM,
            price = 0.0,
            purchaseCount = 200,
            likeCount = 80,
            isDeleted = false,
        )

        product3 = createProduct(
            id = 3L,
            title = "한정판매 패턴",
            category = ProductCategory.OUTER,
            price = 15_000.0,
            purchaseCount = 150,
            likeCount = 60,
            isDeleted = false,
            stockQuantity = 10,
        )

        whenever(stringRedisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(valueOperations.get(any())).thenReturn(null)
    }

    @Test
    @DisplayName("인기 Top5 조회 - Redis 데이터 있음(순서 보존)")
    fun getTop5Products_WithRedis() {
        // given
        val top5Ids = listOf(2L, 3L, 1L)
        whenever(redisProductService.getTopNPopularProducts(5)).thenReturn(top5Ids)
        whenever(productRepository.findByProductIdInAndIsDeletedFalse(top5Ids))
            .thenReturn(listOf(product2, product3, product1))
        whenever(productLikeRepository.findLikedProductIdsByUserId(any(), any()))
            .thenReturn(emptySet())

        // when
        val result = homeSectionService.getPopularTop5(null)

        // then
        assertThat(result).hasSize(3)
        assertThat(result)
            .extracting<Long> { it.productId }
            .containsExactly(2L, 3L, 1L)

        verify(redisProductService).getTopNPopularProducts(5)
        verify(productRepository).findByProductIdInAndIsDeletedFalse(top5Ids)
    }

    @Test
    @DisplayName("인기 Top5 조회 - Redis 데이터 없음(DB 조회로 대체)")
    fun getTop5Products_WithoutRedis() {
        // given
        whenever(redisProductService.getTopNPopularProducts(5))
            .thenReturn(emptyList())

        val top5Page = PageImpl(listOf(product2, product3, product1))
        whenever(productRepository.findByIsDeletedFalse(any()))
            .thenReturn(top5Page)
        whenever(productLikeRepository.findLikedProductIdsByUserId(any(), any()))
            .thenReturn(emptySet())

        // when
        val result = homeSectionService.getPopularTop5(null)

        // then
        assertThat(result).hasSize(3)
        assertThat(result)
            .extracting<Long> { it.productId }
            .containsExactly(2L, 3L, 1L)

        val captor = argumentCaptor<Pageable>()
        verify(productRepository).findByIsDeletedFalse(captor.capture())
        val captured = captor.firstValue

        assertThat(captured.pageNumber).isEqualTo(0)
        assertThat(captured.pageSize).isEqualTo(5)
        val order = captured.sort.getOrderFor("purchaseCount")
        assertThat(order).isNotNull
        assertThat(order?.direction).isEqualTo(Sort.Direction.DESC)
    }

    @Test
    @DisplayName("최신 리뷰 3개 조회 - Repository 결과 반환")
    fun getLatestReviews_ReturnsTop3() {
        // given
        val r1 = LatestReviewItem(101L, 10L, "니트 스웨터", null, 5, "아주 좋아요", LocalDate.now())
        val r2 = LatestReviewItem(102L, 11L, "울 머플러", null, 4, "따뜻합니다", LocalDate.now().minusDays(1))
        val r3 = LatestReviewItem(103L, 12L, "가디건", null, 5, "부드러워요", LocalDate.now().minusDays(2))

        whenever(homeQueryRepository.findLatestReviews(3))
            .thenReturn(listOf(r1, r2, r3))

        // when
        val result = homeSectionService.getLatestReviews(3)

        // then
        assertThat(result).hasSize(3)
        assertThat(result[0].reviewId).isEqualTo(101L)
        assertThat(result[1].productTitle).isEqualTo("울 머플러")

        verify(homeQueryRepository).findLatestReviews(3)
    }

    @Test
    @DisplayName("최신 커뮤니티 글 3개 조회 - Repository 결과 반환")
    fun getLatestPosts_ReturnsTop3() {
        // given
        val p1 = LatestPostItem(201L, "첫 글", "FREE", null, LocalDateTime.now())
        val p2 = LatestPostItem(202L, "둘째 글", "QUESTION", null, LocalDateTime.now().minusHours(1))
        val p3 = LatestPostItem(203L, "셋째 글", "TIP", null, LocalDateTime.now().minusDays(1))

        whenever(homeQueryRepository.findLatestPosts(3))
            .thenReturn(listOf(p1, p2, p3))

        // when
        val result = homeSectionService.getLatestPosts(3)

        // then
        assertThat(result).hasSize(3)
        assertThat(result[0].postId).isEqualTo(201L)
        assertThat(result[2].category).isEqualTo("TIP")

        verify(homeQueryRepository).findLatestPosts(3)
    }

    @Test
    @DisplayName("홈 요약 조회 - 인기 Top5 + 최신 리뷰3 + 최신 글3")
    fun getHomeSummary_AggregatesAllSections() {
        val ids = listOf(2L, 3L, 1L)
        whenever(redisProductService.getTopNPopularProducts(5)).thenReturn(ids)
        whenever(productRepository.findByProductIdInAndIsDeletedFalse(ids))
            .thenReturn(listOf(product2, product3, product1))
        whenever(productLikeRepository.findLikedProductIdsByUserId(any(), any()))
            .thenReturn(emptySet())

        val r1 = LatestReviewItem(101L, 10L, "니트 스웨터", null, 5, "굿", LocalDate.now())
        val r2 = LatestReviewItem(102L, 11L, "울 머플러", null, 4, "따뜻", LocalDate.now())
        val r3 = LatestReviewItem(103L, 12L, "가디건", null, 5, "부드러움", LocalDate.now())
        whenever(homeQueryRepository.findLatestReviews(3))
            .thenReturn(listOf(r1, r2, r3))

        val p1 = LatestPostItem(201L, "첫 글", "FREE", null, LocalDateTime.now())
        val p2 = LatestPostItem(202L, "둘째 글", "QUESTION", null, LocalDateTime.now())
        val p3 = LatestPostItem(203L, "셋째 글", "TIP", null, LocalDateTime.now())
        whenever(homeQueryRepository.findLatestPosts(3))
            .thenReturn(listOf(p1, p2, p3))

        val response: HomeSummaryResponse = homeSectionService.getHomeSummary(null)

        assertThat(response.popularProducts).hasSize(3)
        assertThat(response.latestReviews).hasSize(3)
        assertThat(response.latestPosts).hasSize(3)

        assertThat(response.popularProducts)
            .extracting<Long?> { it.productId }
            .containsExactly(2L, 3L, 1L)

        assertThat(response.latestReviews[0].rating).isEqualTo(5)
        assertThat(response.latestPosts[1].category).isEqualTo("QUESTION")
    }

    private fun createProduct(
        id: Long,
        title: String,
        category: ProductCategory,
        price: Double,
        purchaseCount: Int,
        likeCount: Int,
        isDeleted: Boolean,
        stockQuantity: Int? = null,
    ): Product {
        val user: User = mock()
        val design: Design = mock()

        val product = Product(
            productId = id,
            title = title,
            description = "테스트 설명",
            productCategory = category,
            sizeInfo = "FREE",
            price = price,
            user = user,
            purchaseCount = purchaseCount,
            isDeleted = isDeleted,
            stockQuantity = stockQuantity,
            likeCount = likeCount,
            design = design
        )
        val createdAtField = Product::class.java.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(product, createdAt)

        return product
    }
}
