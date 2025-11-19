// FILE: backend/src/test/java/com/mysite/knitly/domain/home/controller/HomeControllerIntegrationTest.kt
package com.mysite.knitly.domain.home.controller

import com.mysite.knitly.domain.home.dto.HomeSummaryResponse
import com.mysite.knitly.domain.home.dto.LatestPostItem
import com.mysite.knitly.domain.home.dto.LatestReviewItem
import com.mysite.knitly.domain.home.service.HomeSectionService
import com.mysite.knitly.domain.product.product.dto.ProductListResponse
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HomeControllerIntegrationTest(

    @Autowired
    private val mockMvc: MockMvc

) {

    @MockBean
    private lateinit var homeSectionService: HomeSectionService

    @Test
    @DisplayName("GET /home/summary - 홈 요약 조회 성공(비로그인)")
    fun getHomeSummary_success_anonymous() {
        val now = LocalDateTime.now()

        val product1 = ProductListResponse(
            productId = 1L,
            title = "테스트 상품1",
            productCategory = ProductCategory.TOP,
            price = 10000.0,
            purchaseCount = 10,
            likeCount = 5,
            isLikedByUser = false,
            stockQuantity = 10,
            avgReviewRating = 4.5,
            createdAt = now,
            thumbnailUrl = "thumb1.jpg",
            sellerName = "판매자1",
            isFree = false,
            isLimited = true,
            isSoldOut = false,
            userId = 100L
        )

        val latestReview = LatestReviewItem(
            reviewId = 11L,
            productId = 1L,
            productTitle = "테스트 상품1",
            productThumbnailUrl = "thumb1.jpg",
            rating = 5,
            content = "아주 좋아요",
            createdDate = LocalDate.now()
        )

        val latestPost = LatestPostItem(
            postId = 21L,
            title = "첫 글",
            category = "FREE",
            thumbnailUrl = null,
            createdAt = now
        )

        val summary = HomeSummaryResponse(
            popularProducts = listOf(product1),
            latestReviews = listOf(latestReview),
            latestPosts = listOf(latestPost)
        )

        Mockito.`when`(homeSectionService.getHomeSummary(null))
            .thenReturn(summary)

        mockMvc.get("/home/summary")
            .andExpect {
                status { isOk() }
                jsonPath("$.popularProducts[0].productId") {
                    value(product1.productId!!.toInt())
                }
                jsonPath("$.popularProducts[0].title") {
                    value("테스트 상품1")
                }
                jsonPath("$.latestReviews[0].reviewId") {
                    value(latestReview.reviewId.toInt())
                }
                jsonPath("$.latestPosts[0].postId") {
                    value(latestPost.postId.toInt())
                }
            }

        verify(homeSectionService).getHomeSummary(null)
    }
}
