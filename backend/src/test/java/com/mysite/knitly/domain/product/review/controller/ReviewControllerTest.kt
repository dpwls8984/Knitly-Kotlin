package com.mysite.knitly.domain.product.review.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.entity.OrderItem
import com.mysite.knitly.domain.order.repository.OrderItemRepository
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.product.review.entity.Review
import com.mysite.knitly.domain.product.review.repository.ReviewRepository
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.utility.jwt.JwtProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Disabled("통합 테스트는 잠시 비활성화")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var productRepository: ProductRepository
    @Autowired
    lateinit var designRepository: DesignRepository
    @Autowired
    lateinit var reviewRepository: ReviewRepository
    @Autowired
    lateinit var orderRepository: OrderRepository
    @Autowired
    lateinit var orderItemRepository: OrderItemRepository
    @Autowired
    lateinit var jwtProvider: JwtProvider

    private lateinit var testUser: User
    private lateinit var otherUser: User
    private lateinit var testProduct: Product
    private lateinit var testDesign: Design
    private lateinit var testOrder: Order
    private lateinit var testOrderItem: OrderItem
    private lateinit var testUserToken: String
    private lateinit var otherUserToken: String

    @BeforeEach
    fun setUp() {
        testUser = userRepository.save(
            User(
                socialId = "google_123456789",
                email = "test@test.com",
                name = "testUser",
                provider = Provider.GOOGLE
            )
        )

        otherUser = userRepository.save(
            User(
                socialId = "google_987654321",
                email = "other@test.com",
                name = "otherUser",
                provider = Provider.GOOGLE
            )
        )

        testDesign = designRepository.save(
            Design(
                user = testUser,
                designName = "테스트 도안",
                designState = DesignState.BEFORE_SALE,
                gridData = "{}",
                createdAt = LocalDateTime.now()
            )
        )

        testProduct = productRepository.save(
            Product(
                title = "테스트 상품",
                description = "이것은 테스트 상품입니다.",
                productCategory = ProductCategory.TOP,
                sizeInfo = "Free",
                price = 10000.0,
                user = testUser,
                purchaseCount = 0,
                isDeleted = false,
                stockQuantity = 100,
                likeCount = 0,
                design = testDesign
            )
        )

        testOrderItem = OrderItem(
            product = testProduct,
            orderPrice = testProduct.price,
            quantity = 1
        )

        testOrder = Order.create(
            user = testUser,
            items = listOf(testOrderItem)
        )
        orderRepository.save(testOrder)

        testUserToken = jwtProvider.createAccessToken(testUser.userId)
        otherUserToken = jwtProvider.createAccessToken(otherUser.userId)
    }

    private fun postReview(productId: Long?, token: String, content: String, rating: Int) =
        mockMvc.perform(
            post("/products/$productId/reviews")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("content", content)
                .param("rating", rating.toString())
        )

    private fun deleteReview(reviewId: Long, token: String) =
        mockMvc.perform(
            delete("/reviews/$reviewId")
                .header("Authorization", "Bearer $token")
        )

    @Test
    @DisplayName("리뷰 등록: 성공")
    fun createReview_Success() {
        val content = "리뷰 내용입니다."
        val rating = 5

        postReview(testProduct.productId, testUserToken, content, rating)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value(content))
            .andExpect(jsonPath("$.rating").value(rating))
            .andExpect(jsonPath("$.userName").value(testUser.name))
            .andExpect(jsonPath("$.reviewId").isNumber)
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.reviewImageUrls").isArray)
    }

    @Test
    @DisplayName("리뷰 등록: 인증 실패 (토큰 없음)")
    fun createReview_Fail_NoToken() {
        mockMvc.perform(
            post("/products/${testProduct.productId}/reviews")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("content", "내용")
                .param("rating", "5")
        ).andExpect(status().isFound) // 302 Redirect
    }

    @Test
    @DisplayName("리뷰 삭제: 성공")
    fun deleteReview_Success() {
        val review = reviewRepository.save(
            Review(
                user = testUser,
                product = testProduct,
                content = "삭제될 리뷰",
                rating = 5,
                orderItem = testOrderItem
            )
        )

        deleteReview(review.reviewId, testUserToken)
            .andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("리뷰 삭제: 인가 실패 (권한 없는 사용자)")
    fun deleteReview_Fail_NotOwner() {
        val review = reviewRepository.save(
            Review(
                user = testUser,
                product = testProduct,
                content = "삭제될 리뷰",
                rating = 5,
                orderItem = testOrderItem
            )
        )

        deleteReview(review.reviewId, otherUserToken)
            .andExpect(status().isForbidden)
    }
}