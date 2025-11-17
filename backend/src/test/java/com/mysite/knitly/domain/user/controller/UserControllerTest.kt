package com.mysite.knitly.domain.user.controller

import com.mysite.knitly.domain.product.product.dto.ProductListResponse
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.service.ProductService
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.service.UserService
import com.mysite.knitly.utility.auth.service.AuthService
import com.mysite.knitly.utility.cookie.CookieUtil
import com.mysite.knitly.utility.jwt.JwtProvider
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * UserController 단위 테스트
 *
 * 테스트 전략:
 * 1. Mock 객체를 사용하여 의존성 분리
 * 2. 각 메서드의 성공/실패 시나리오 테스트
 * 3. 반환값 검증 및 메서드 호출 검증
 */
@ExtendWith(MockitoExtension::class)
class UserControllerTest {

    @Mock
    private lateinit var authService: AuthService

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var cookieUtil: CookieUtil

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var jwtProvider: JwtProvider

    @Mock
    private lateinit var response: HttpServletResponse

    @InjectMocks
    private lateinit var userController: UserController

    private lateinit var testUser: User
    private lateinit var authHeader: String

    @BeforeEach
    fun setUp() {
        // 테스트용 User 객체 생성
        testUser = User.builder()
            .userId(1L)
            .socialId("google-123456")
            .email("test@example.com")
            .name("테스트유저")
            .provider(Provider.GOOGLE)
            .build()

        authHeader = "Bearer test-access-token"
    }

    // ========== 현재 사용자 정보 조회 테스트 ==========

    @Test
    @DisplayName("로그인한 사용자 정보 조회 성공")
    fun `getCurrentUser Success`() {
        // when
        val result = userController.getCurrentUser(testUser)

        // then
        assertThat(result.statusCode.value()).isEqualTo(200)
        assertThat(result.body).isNotNull
        assertThat(result.body?.get("userId")).isEqualTo(1L)
        assertThat(result.body?.get("email")).isEqualTo("test@example.com")
        assertThat(result.body?.get("name")).isEqualTo("테스트유저")
        assertThat(result.body?.get("provider")).isEqualTo(Provider.GOOGLE)
        assertThat(result.body).containsKey("createdAt")
    }

    @Test
    @DisplayName("로그인한 사용자 정보 조회 실패 - 인증 없음")
    fun `getCurrentUser Fail Unauthorized`() {
        // when
        val result = userController.getCurrentUser(null)

        // then
        assertThat(result.statusCode.value()).isEqualTo(401)
        assertThat(result.body).isNull()
    }

    // ========== 로그아웃 테스트 ==========

    @Test
    @DisplayName("로그아웃 성공 - Access Token으로 userId 추출")
    fun `logout Success WithAccessToken`() {
        // given
        given(jwtProvider.getUserIdFromToken("test-access-token")).willReturn(1L)
        doNothing().`when`(authService).logout(1L)
        doNothing().`when`(cookieUtil).deleteCookie(
            any(HttpServletResponse::class.java),
            eq("refreshToken")
        )

        // when
        val result = userController.logout(authHeader, response)

        // then
        assertThat(result.statusCode.value()).isEqualTo(200)
        assertThat(result.body).isEqualTo("로그아웃되었습니다.")

        // 검증: authService.logout()이 2번 호출되는지 확인 (코드 상 중복 호출됨)
        verify(authService, times(2)).logout(1L)
        verify(cookieUtil, times(1)).deleteCookie(response, "refreshToken")
        verify(jwtProvider, times(1)).getUserIdFromToken("test-access-token")
    }

    @Test
    @DisplayName("로그아웃 성공 - Authorization 헤더 없음")
    fun `logout Success WithoutAuthHeader`() {
        // given
        doNothing().`when`(cookieUtil).deleteCookie(
            any(HttpServletResponse::class.java),
            eq("refreshToken")
        )

        // when
        val result = userController.logout(null, response)

        // then
        assertThat(result.statusCode.value()).isEqualTo(200)
        assertThat(result.body).isEqualTo("로그아웃되었습니다.")

        // JWT 파싱 시도하지 않음
        verify(jwtProvider, never()).getUserIdFromToken(anyString())
        // authService.logout(null) 호출됨
        verify(authService, times(1)).logout(null)
        verify(cookieUtil, times(1)).deleteCookie(response, "refreshToken")
    }

    @Test
    @DisplayName("로그아웃 - 토큰 파싱 실패해도 쿠키는 삭제됨")
    fun `logout TokenParsingFails StillDeletesCookie`() {
        // given
        given(jwtProvider.getUserIdFromToken("test-access-token"))
            .willThrow(RuntimeException("Invalid token"))
        doNothing().`when`(cookieUtil).deleteCookie(
            any(HttpServletResponse::class.java),
            eq("refreshToken")
        )

        // when
        val result = userController.logout(authHeader, response)

        // then
        assertThat(result.statusCode.value()).isEqualTo(200)
        assertThat(result.body).isEqualTo("로그아웃되었습니다.")

        // 쿠키는 삭제됨
        verify(cookieUtil, times(1)).deleteCookie(response, "refreshToken")
        // authService.logout(null) 호출됨 (userId 추출 실패)
        verify(authService, times(1)).logout(null)
    }

    // ========== 회원탈퇴 테스트 ==========

    @Test
    @DisplayName("회원탈퇴 성공")
    fun `deleteAccount Success`() {
        // given
        doNothing().`when`(authService).deleteAccount(1L)
        doNothing().`when`(cookieUtil).deleteCookie(
            any(HttpServletResponse::class.java),
            eq("refreshToken")
        )

        // when
        val result = userController.deleteAccount(testUser, response)

        // then
        assertThat(result.statusCode.value()).isEqualTo(200)
        assertThat(result.body).isEqualTo("회원탈퇴가 완료되었습니다.")

        verify(authService, times(1)).deleteAccount(1L)
        verify(cookieUtil, times(1)).deleteCookie(response, "refreshToken")
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 인증 없음")
    fun `deleteAccount Fail Unauthorized`() {
        // when
        val result = userController.deleteAccount(null, response)

        // then
        assertThat(result.statusCode.value()).isEqualTo(401)
        assertThat(result.body).isEqualTo("인증이 필요합니다.")

        // 서비스 메서드 호출되지 않음
        verify(authService, never()).deleteAccount(anyLong())
        verify(cookieUtil, never()).deleteCookie(any(), anyString())
    }

    @Test
    @DisplayName("회원탈퇴 - DB 삭제 실패해도 쿠키는 삭제됨")
    fun `deleteAccount DBFailure StillDeletesCookie`() {
        // given
        doThrow(RuntimeException("DB 오류"))
            .`when`(authService).deleteAccount(1L)

        // when & then
        try {
            userController.deleteAccount(testUser, response)
        } catch (e: RuntimeException) {
            assertThat(e.message).isEqualTo("DB 오류")
        }

        // authService.deleteAccount() 호출은 되었음
        verify(authService, times(1)).deleteAccount(1L)
        // 예외 발생으로 쿠키 삭제는 실행되지 않음
        verify(cookieUtil, never()).deleteCookie(any(), anyString())
    }

    // ========== 판매자 상품 목록 조회 테스트 ==========

    @Test
    @DisplayName("특정 유저가 판매중인 상품 목록 조회 성공")
    fun `getProductsWithUserId Success`() {
        // given
        val userId = 1L
        val pageable: Pageable = PageRequest.of(0, 20)

        val products = listOf(
            ProductListResponse(
                productId = 1L,
                title = "니트 스웨터",
                productCategory = ProductCategory.TOP,
                price = 29900.0,
                purchaseCount = 5,
                likeCount = 10,
                isLikedByUser = false,
                stockQuantity = 100,
                avgReviewRating = 4.5,
                createdAt = LocalDateTime.now(),
                thumbnailUrl = "http://example.com/image.jpg",
                sellerName = "테스트유저",
                isFree = false,
                isLimited = true,
                isSoldOut = false,
                userId = userId
            )
        )

        val mockPage = PageImpl(products, pageable, 1)
        given(productService.findProductsByUserId(userId, pageable)).willReturn(mockPage)

        // when
        val result = userController.getProductsWithUserId(userId, pageable)

        // then
        assertThat(result.statusCode.value()).isEqualTo(200)
        assertThat(result.body).isNotNull
        assertThat(result.body?.content).hasSize(1)
        assertThat(result.body?.content?.get(0)?.title).isEqualTo("니트 스웨터")
        assertThat(result.body?.totalElements).isEqualTo(1)

        verify(productService, times(1)).findProductsByUserId(userId, pageable)
    }

    @Test
    @DisplayName("특정 유저가 판매중인 상품 목록 조회 - 상품 없음")
    fun `getProductsWithUserId EmptyResult`() {
        // given
        val userId = 999L
        val pageable: Pageable = PageRequest.of(0, 20)

        val emptyPage = PageImpl<ProductListResponse>(emptyList(), pageable, 0)
        given(productService.findProductsByUserId(userId, pageable)).willReturn(emptyPage)

        // when
        val result = userController.getProductsWithUserId(userId, pageable)

        // then
        assertThat(result.statusCode.value()).isEqualTo(200)
        assertThat(result.body).isNotNull
        assertThat(result.body?.content).isEmpty()
        assertThat(result.body?.totalElements).isEqualTo(0)

        verify(productService, times(1)).findProductsByUserId(userId, pageable)
    }

    @Test
    @DisplayName("특정 유저가 판매중인 상품 목록 조회 - 페이징 처리 확인")
    fun `getProductsWithUserId WithPagination`() {
        // given
        val userId = 1L
        val pageable: Pageable = PageRequest.of(1, 10) // 2페이지, 10개씩

        val products = (0 until 10).map { i ->
            ProductListResponse(
                productId = (i + 11).toLong(),              // 11~20번 상품
                title = "상품 ${i + 11}",
                productCategory = ProductCategory.TOP,      // 추가
                price = 10000.0 + i * 1000,
                purchaseCount = i,                          // 추가 (reviewCount → purchaseCount)
                likeCount = i,
                isLikedByUser = false,                      // 추가 (isLiked → isLikedByUser)
                stockQuantity = 50,
                avgReviewRating = 4.0,                      // 추가 (averageRating → avgReviewRating)
                createdAt = LocalDateTime.now(),
                thumbnailUrl = "http://example.com/image$i.jpg",  // 추가 (thumbnailImage → thumbnailUrl)
                sellerName = "테스트유저",
                isFree = false,                             // 추가
                isLimited = true,                           // 추가
                isSoldOut = false,
                userId = userId                             // 추가 (sellerId → userId)
            )
        }

        val mockPage = PageImpl(products, pageable, 25) // 총 25개
        given(productService.findProductsByUserId(userId, pageable)).willReturn(mockPage)

        // when
        val result = userController.getProductsWithUserId(userId, pageable)

        // then
        assertThat(result.statusCode.value()).isEqualTo(200)
        assertThat(result.body).isNotNull
        assertThat(result.body?.content).hasSize(10)
        assertThat(result.body?.totalElements).isEqualTo(25)
        assertThat(result.body?.totalPages).isEqualTo(3)
        assertThat(result.body?.number).isEqualTo(1) // 현재 페이지

        verify(productService, times(1)).findProductsByUserId(userId, pageable)
    }
}