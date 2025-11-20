package com.mysite.knitly.domain.mypage.controller

import com.mysite.knitly.domain.mypage.dto.*
import com.mysite.knitly.domain.mypage.service.MyPageService
import com.mysite.knitly.domain.payment.service.PaymentService
import com.mysite.knitly.domain.user.entity.User
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.core.MethodParameter
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.time.LocalDate
import java.time.LocalDateTime

class MyPageControllerTest {

    private lateinit var mvc: MockMvc
    private lateinit var service: MyPageService
    private lateinit var paymentService: PaymentService
    private lateinit var principal: User

    @BeforeEach
    fun setUp() {
        service = Mockito.mock(MyPageService::class.java)
        paymentService = Mockito.mock(PaymentService::class.java)

        val controller = MyPageController(service, paymentService)

        val forceUserResolver = object : HandlerMethodArgumentResolver {
            override fun supportsParameter(parameter: MethodParameter): Boolean {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal::class.java) &&
                        User::class.java.isAssignableFrom(parameter.parameterType)
            }

            override fun resolveArgument(
                parameter: MethodParameter,
                mavContainer: ModelAndViewContainer?,
                webRequest: NativeWebRequest,
                binderFactory: WebDataBinderFactory?
            ): Any {
                return principal
            }
        }

        mvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setCustomArgumentResolvers(forceUserResolver)
            .build()

        principal = Mockito.mock(User::class.java)
        whenever(principal.userId).thenReturn(1L)
        whenever(principal.name).thenReturn("홍길동")
        whenever(principal.email).thenReturn("hong@example.com")
    }

    @Test
    @DisplayName("GET /mypage/profile → profile 반환")
    fun profile() {
        mvc.perform(get("/mypage/profile"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.email").value("hong@example.com"))
    }

    @Test
    @DisplayName("GET /mypage/orders → 주문 카드 목록 반환")
    fun orders() {
        val baseCard = OrderCardResponse.of(
            101L,
            LocalDateTime.of(2025, 1, 2, 10, 0),
            30000.0
        )

        val items = listOf(
            OrderLine(
                11L, 1001L, "도안 A", 1, 10000.0, false
            ),
            OrderLine(
                12L, 1002L, "도안 B", 2, 20000.0, true
            )
        )

        val card = baseCard.copy(items = items)

        val page = PageImpl(listOf(card), PageRequest.of(0, 3), 1)

        whenever(service.getOrderCards(eq(1L), any()))
            .thenReturn(page)

        mvc.perform(
            get("/mypage/orders")
                .param("page", "0")
                .param("size", "3")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content", hasSize<Any>(1)))
            .andExpect(jsonPath("$.content[0].orderId").value(101))
            .andExpect(jsonPath("$.content[0].items", hasSize<Any>(2)))
            .andExpect(jsonPath("$.content[0].items[0].productTitle").value("도안 A"))
    }

    @Test
    @DisplayName("GET /mypage/posts → 내가 쓴 글 목록")
    fun myPosts() {
        val dto = MyPostListItemResponse(
            501L, "제목1", "요약1", "thumb1.jpg",
            LocalDateTime.of(2025, 1, 3, 9, 0)
        )

        val page = PageImpl(listOf(dto), PageRequest.of(0, 10), 1)

        whenever(service.getMyPosts(eq(1L), eq("키워드"), any()))
            .thenReturn(page)

        mvc.perform(
            get("/mypage/posts")
                .param("query", "키워드")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(501))
            .andExpect(jsonPath("$.content[0].title").value("제목1"))
    }

    @Test
    @DisplayName("GET /mypage/comments → 내가 쓴 댓글 목록")
    fun myComments() {
        val dto = MyCommentListItem(
            701L, 501L,
            LocalDateTime.of(2025, 1, 4, 12, 0),
            "내용 미리보기"
        )

        val page = PageImpl(listOf(dto), PageRequest.of(0, 10), 1)

        whenever(service.getMyComments(eq(1L), eq("단어"), any()))
            .thenReturn(page)

        mvc.perform(
            get("/mypage/comments")
                .param("query", "단어")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].commentId").value(701))
            .andExpect(jsonPath("$.content[0].postId").value(501))
            .andExpect(jsonPath("$.content[0].preview").value("내용 미리보기"))
    }

    @Test
    @DisplayName("GET /mypage/favorites → 내가 찜한 상품 목록")
    fun myFavorites() {
        val f = FavoriteProductItem(
            9001L,
            "인기 도안",
            "홍길동",
            "t.jpg"
        )

        val page = PageImpl(listOf(f), PageRequest.of(0, 10), 1)

        whenever(service.getMyFavorites(eq(1L), any()))
            .thenReturn(page)

        mvc.perform(
            get("/mypage/favorites")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].productId").value(9001))
            .andExpect(jsonPath("$.content[0].productTitle").value("인기 도안"))
            .andExpect(jsonPath("$.content[0].sellerName").value("홍길동"))
            .andExpect(jsonPath("$.content[0].thumbnailUrl").value("t.jpg"))
    }

    @Test
    @DisplayName("GET /mypage/reviews → 내가 작성한 리뷰 목록")
    fun myReviews() {
        val r = ReviewListItem(
            301L,
            9001L,
            "인기 도안",
            "t.jpg",
            5,
            "좋아요",
            listOf("r1.jpg", "r2.jpg"),
            LocalDate.of(2025, 1, 6)
        )

        val page = PageImpl(listOf(r), PageRequest.of(0, 10), 1)

        whenever(service.getMyReviewsV2(eq(1L), any()))
            .thenReturn(page)

        mvc.perform(
            get("/mypage/reviews")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].reviewId").value(301))
            .andExpect(jsonPath("$.content[0].rating").value(5))
            .andExpect(jsonPath("$.content[0].reviewImageUrls", hasSize<Any>(2)))
    }
}
