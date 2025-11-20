package com.mysite.knitly.domain.review.controller

import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest
import com.mysite.knitly.domain.product.review.dto.ReviewCreateResponse
import com.mysite.knitly.domain.product.review.dto.ReviewListResponse
import com.mysite.knitly.domain.product.review.service.ReviewService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.util.TestSecurityUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ReviewControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var reviewService: ReviewService

    // Redis 연결 방지
    @MockBean
    private lateinit var redissonClient: RedissonClient

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        user = User.builder()
            .userId(1L)
            .socialId("social123")
            .email("test@example.com")
            .name("TestUser")
            .provider(Provider.GOOGLE)
            .build()
    }

    @Test
    @DisplayName("GET /reviews/form - 리뷰 작성 폼 조회 성공")
    fun getReviewForm_Success() {
        val orderItemId = 10L

        // ⬇ DTO 최신 구조에 맞게 수정됨
        val response = ReviewCreateResponse(
            productTitle = "테스트 니트",
            productThumbnailUrl = "image.jpg"
        )

        Mockito.`when`(reviewService.getReviewFormInfo(orderItemId))
            .thenReturn(response)

        mockMvc.get("/reviews/form") {
            param("orderItemId", orderItemId.toString())
        }
            .andExpect { status { isOk() } }

        verify(reviewService).getReviewFormInfo(orderItemId)
    }

    @Test
    @DisplayName("POST /reviews - 리뷰 생성 성공")
    fun createReview_Success() {
        val orderItemId = 10L

        val request = ReviewCreateRequest(
            rating = 5,
            content = "아주 좋아요!"
        )

        mockMvc.post("/reviews") {
            with(TestSecurityUtil.createPrincipal(user)!!)
            param("orderItemId", orderItemId.toString())
            contentType = MediaType.MULTIPART_FORM_DATA
            param("rating", request.rating.toString())
            param("content", request.content)
        }
            .andExpect { status { isOk() } }

        verify(reviewService).createReview(orderItemId, user, request)
    }

    @Test
    @DisplayName("DELETE /reviews/{reviewId} - 리뷰 삭제 성공")
    fun deleteReview_Success() {
        val reviewId = 55L

        mockMvc.delete("/reviews/$reviewId") {
            with(TestSecurityUtil.createPrincipal(user)!!)
        }
            .andExpect { status { isNoContent() } }

        verify(reviewService).deleteReview(reviewId, user)
    }

    @Test
    @DisplayName("GET /products/{productId}/reviews - 상품 리뷰 목록 조회 성공")
    fun getReviewListByProduct_Success() {
        val productId = 100L

        val now = LocalDateTime.now()

        val list = listOf(
            ReviewListResponse(
                reviewId = 1L,
                rating = 5,
                content = "좋습니다!",
                createdAt = now,
                userName = "사용자A",
                reviewImageUrls = listOf("img1.jpg", "img2.jpg")
            ),
            ReviewListResponse(
                reviewId = 2L,
                rating = 4,
                content = "괜찮아요.",
                createdAt = now.minusDays(1),
                userName = "사용자B",
                reviewImageUrls = emptyList()
            )
        )

        val page: Page<ReviewListResponse> = PageImpl(list)

        Mockito.`when`(reviewService.getReviewsByProduct(productId, 0, 10))
            .thenReturn(page)

        mockMvc.get("/products/$productId/reviews") {
            param("page", "0")
            param("size", "10")
        }
            .andExpect { status { isOk() } }

        verify(reviewService).getReviewsByProduct(productId, 0, 10)
    }
}