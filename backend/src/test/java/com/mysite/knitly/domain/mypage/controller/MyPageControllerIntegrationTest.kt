package com.mysite.knitly.domain.mypage.controller

import com.mysite.knitly.domain.mypage.dto.*
import com.mysite.knitly.domain.mypage.service.MyPageService
import com.mysite.knitly.domain.payment.dto.PaymentDetailResponse
import com.mysite.knitly.domain.payment.service.PaymentService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.util.TestSecurityUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyPageControllerIntegrationTest(

    @Autowired
    private val mockMvc: MockMvc

) {

    // Redis 연동 방지 (팀원 패턴과 동일)
    @MockBean
    lateinit var redissonClient: RedissonClient

    // 서비스들은 MockBean 으로 주입 (DB는 안 타고 호출/응답만 검증)
    @MockBean
    lateinit var myPageService: MyPageService

    @MockBean
    lateinit var paymentService: PaymentService

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        user = User.builder()
            .userId(1L)
            .socialId("mypage-int-test-social")
            .email("mypage@test.com")
            .name("MyPageTester")
            .provider(Provider.GOOGLE)
            .build()
    }

    // profile

    @Test
    @DisplayName("GET /mypage/profile - 프로필 조회 성공")
    fun getProfile_Success() {
        mockMvc.perform(
            get("/mypage/profile")
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(user.name))
            .andExpect(jsonPath("$.email").value(user.email))
    }

    // orders

    @Test
    @DisplayName("GET /mypage/orders - 주문 내역 조회 성공")
    fun getOrders_Success() {
        val page = 0
        val size = 3
        val pageable = PageRequest.of(page, size)

        val now = LocalDateTime.now()

        val cards: List<OrderCardResponse> = listOf(
            OrderCardResponse(
                orderId = 10L,
                orderedAt = now,
                totalPrice = 30000.0,
                items = listOf(
                    OrderLine(
                        orderItemId = 100L,
                        productId = 1L,
                        productTitle = "테스트 상품 1",
                        quantity = 2,
                        orderPrice = 15000.0,
                        isReviewed = false
                    )
                )
            )
        )

        val pageResult: Page<OrderCardResponse> = PageImpl(cards, pageable, cards.size.toLong())

        given(myPageService.getOrderCards(user.userId, pageable))
            .willReturn(pageResult)

        mockMvc.perform(
            get("/mypage/orders")
                .param("page", page.toString())
                .param("size", size.toString())
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].orderId").value(10L))
            .andExpect(jsonPath("$.content[0].items[0].productTitle").value("테스트 상품 1"))

        verify(myPageService, times(1)).getOrderCards(user.userId, pageable)
    }

    // orders/{id}/payment

    @Test
    @DisplayName("GET /mypage/orders/{orderId}/payment - 결제 상세 조회 성공")
    fun getOrderPayment_Success() {
        val orderId = 10L

        // PaymentDetailResponse 구조를 몰라도 mock 으로 충분
        val detail = Mockito.mock(PaymentDetailResponse::class.java)

        given(paymentService.getPaymentDetailByOrder(user, orderId))
            .willReturn(detail)

        mockMvc.perform(
            get("/mypage/orders/{orderId}/payment", orderId)
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isOk)

        verify(paymentService, times(1)).getPaymentDetailByOrder(user, orderId)
    }

    // posts

    @Test
    @DisplayName("GET /mypage/posts - 내 글 목록 조회 성공")
    fun getMyPosts_Success() {
        val page = 0
        val size = 10
        val query = "테스트"
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val now = LocalDateTime.now()

        val list = listOf(
            MyPostListItemResponse(
                id = 1L,
                title = "테스트 글",
                excerpt = "내용 일부...",
                thumbnailUrl = "thumb.jpg",
                createdAt = now
            )
        )

        val pageResult: Page<MyPostListItemResponse> = PageImpl(list, pageable, list.size.toLong())

        given(myPageService.getMyPosts(user.userId, query, pageable))
            .willReturn(pageResult)

        mockMvc.perform(
            get("/mypage/posts")
                .param("query", query)
                .param("page", page.toString())
                .param("size", size.toString())
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(1L))
            .andExpect(jsonPath("$.content[0].title").value("테스트 글"))

        verify(myPageService, times(1)).getMyPosts(user.userId, query, pageable)
    }

    // comments

    @Test
    @DisplayName("GET /mypage/comments - 내 댓글 목록 조회 성공")
    fun getMyComments_Success() {
        val page = 0
        val size = 10
        val query = "댓글"
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val now = LocalDateTime.now()

        val list = listOf(
            MyCommentListItem(
                commentId = 100L,
                postId = 1L,
                createdAt = now,
                preview = "댓글 내용 미리보기"
            )
        )

        val pageResult: Page<MyCommentListItem> = PageImpl(list, pageable, list.size.toLong())

        given(myPageService.getMyComments(user.userId, query, pageable))
            .willReturn(pageResult)

        mockMvc.perform(
            get("/mypage/comments")
                .param("query", query)
                .param("page", page.toString())
                .param("size", size.toString())
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].commentId").value(100L))
            .andExpect(jsonPath("$.content[0].preview").value("댓글 내용 미리보기"))

        verify(myPageService, times(1)).getMyComments(user.userId, query, pageable)
    }

    // favorites

    @Test
    @DisplayName("GET /mypage/favorites - 내 찜 목록 조회 성공")
    fun getMyFavorites_Success() {
        val page = 0
        val size = 10
        val pageable = PageRequest.of(page, size)

        val favorites = listOf(
            FavoriteProductItem(
                productId = 1L,
                productTitle = "니트 테스트 상품",
                sellerName = "판매자1",
                thumbnailUrl = "fav-thumb.jpg"
            )
        )

        val pageResult: Page<FavoriteProductItem> =
            PageImpl(favorites, pageable, favorites.size.toLong())

        given(myPageService.getMyFavorites(user.userId, pageable))
            .willReturn(pageResult)

        mockMvc.perform(
            get("/mypage/favorites")
                .param("page", page.toString())
                .param("size", size.toString())
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].productId").value(1L))
            .andExpect(jsonPath("$.content[0].productTitle").value("니트 테스트 상품"))

        verify(myPageService, times(1)).getMyFavorites(user.userId, pageable)
    }

    // reviews

    @Test
    @DisplayName("GET /mypage/reviews - 내 리뷰 목록 조회 성공")
    fun getMyReviews_Success() {
        val page = 0
        val size = 10
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val today = LocalDate.now()

        val list = listOf(
            ReviewListItem(
                reviewId = 1L,
                productId = 10L,
                productTitle = "리뷰 대상 상품",
                productThumbnailUrl = "review-thumb.jpg",
                rating = 5,
                content = "아주 좋은 상품입니다.",
                reviewImageUrls = listOf("r1.jpg", "r2.jpg"),
                createdDate = today
            )
        )

        val pageResult: Page<ReviewListItem> = PageImpl(list, pageable, list.size.toLong())

        given(myPageService.getMyReviewsV2(user.userId, pageable))
            .willReturn(pageResult)

        mockMvc.perform(
            get("/mypage/reviews")
                .param("page", page.toString())
                .param("size", size.toString())
                .with(TestSecurityUtil.createPrincipal(user)!!)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].reviewId").value(1L))
            .andExpect(jsonPath("$.content[0].productTitle").value("리뷰 대상 상품"))
            .andExpect(jsonPath("$.content[0].rating").value(5))

        verify(myPageService, times(1)).getMyReviewsV2(user.userId, pageable)
    }
}

// Mockito any() 헬퍼 (Kotlin 제네릭 대응)
@Suppress("UNCHECKED_CAST")
private fun <T> any(): T {
    Mockito.any<T>()
    return null as T
}
