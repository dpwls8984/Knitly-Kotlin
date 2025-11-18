package com.mysite.knitly.utility.auth.controller

import com.mysite.knitly.utility.auth.dto.TokenRefreshResponse
import com.mysite.knitly.utility.auth.service.AuthService
import com.mysite.knitly.utility.cookie.CookieUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

/**
 * AuthController 단위 테스트
 *
 * 테스트 대상: Access Token 갱신 API
 * - Refresh Token 검증
 * - 새로운 토큰 발급
 * - 쿠키 처리
 */
@ExtendWith(MockitoExtension::class)
class AuthControllerTest {

    @Mock
    private lateinit var authService: AuthService

    @Mock
    private lateinit var cookieUtil: CookieUtil

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @InjectMocks
    private lateinit var authController: AuthController

    private lateinit var validRefreshToken: String
    private lateinit var mockTokenResponse: TokenRefreshResponse

    companion object {
        private const val REFRESH_TOKEN_COOKIE_NAME = "refreshToken"
        private const val REFRESH_TOKEN_EXPIRE_SECONDS = 604800 // 7일
    }

    @BeforeEach
    fun setUp() {
        // @Value 필드 주입 (ReflectionTestUtils 사용)
        ReflectionTestUtils.setField(
            authController,
            "refreshTokenExpireSeconds",
            REFRESH_TOKEN_EXPIRE_SECONDS
        )

        // 테스트 데이터 준비
        validRefreshToken = "valid.refresh.token.jwt.string"

        mockTokenResponse = TokenRefreshResponse.of(
            accessToken = "new.access.token.jwt.string",
            refreshToken = "new.refresh.token.jwt.string",
            expiresIn = 1800L  // 30분
        )
    }

    // ========== 토큰 갱신 성공 테스트 ==========

    @Test
    @DisplayName("토큰 갱신 성공 - 유효한 Refresh Token")
    fun `refreshToken Success`() {
        // given
        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
            .willReturn(validRefreshToken)

        given(authService.refreshAccessToken(validRefreshToken))
            .willReturn(mockTokenResponse)

        doNothing().`when`(cookieUtil).addCookie(
            any(HttpServletResponse::class.java),
            eq(REFRESH_TOKEN_COOKIE_NAME),
            anyString(),
            anyInt()
        )

        // when
        val result = authController.refreshToken(request, response)

        // then
        assertThat(result.statusCode.value()).isEqualTo(200)
        assertThat(result.body).isNotNull
        assertThat(result.body?.accessToken).isEqualTo("new.access.token.jwt.string")
        assertThat(result.body?.refreshToken).isEqualTo("new.refresh.token.jwt.string")
        assertThat(result.body?.tokenType).isEqualTo("Bearer")
        assertThat(result.body?.expiresIn).isEqualTo(1800L)

        // 검증
        verify(cookieUtil, times(1)).getCookie(request, REFRESH_TOKEN_COOKIE_NAME)
        verify(authService, times(1)).refreshAccessToken(validRefreshToken)
        verify(cookieUtil, times(1)).addCookie(
            response,
            REFRESH_TOKEN_COOKIE_NAME,
            "new.refresh.token.jwt.string",
            REFRESH_TOKEN_EXPIRE_SECONDS
        )
    }

//    // ========== 토큰 갱신 실패 테스트 ==========
//
//    @Test
//    @DisplayName("토큰 갱신 실패 - Refresh Token이 쿠키에 없음")
//    fun `refreshToken Fail NoCookie`() {
//        // given
//        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
//            .willReturn(null)
//
//        // when
//        val result = authController.refreshToken(request, response)
//
//        // then
//        assertThat(result.statusCode.value()).isEqualTo(400)
//        assertThat(result.body).isNull()
//
//        // 검증: authService는 호출되지 않음
//        verify(cookieUtil, times(1)).getCookie(request, REFRESH_TOKEN_COOKIE_NAME)
//        verify(authService, never()).refreshAccessToken(anyString())
//        verify(cookieUtil, never()).addCookie(any(), anyString(), anyString(), anyInt())
//    }
//
//    @Test
//    @DisplayName("토큰 갱신 실패 - 유효하지 않은 Refresh Token")
//    fun `refreshToken Fail InvalidToken`() {
//        // given
//        val invalidToken = "invalid.refresh.token"
//
//        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
//            .willReturn(invalidToken)
//
//        given(authService.refreshAccessToken(invalidToken))
//            .willThrow(IllegalArgumentException("유효하지 않은 Refresh Token입니다."))
//
//        doNothing().`when`(cookieUtil).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME)
//
//        // when
//        val result = authController.refreshToken(request, response)
//
//        // then
//        assertThat(result.statusCode.value()).isEqualTo(401)
//        assertThat(result.body).isNull()
//
//        // 검증: 쿠키 삭제됨
//        verify(cookieUtil, times(1)).getCookie(request, REFRESH_TOKEN_COOKIE_NAME)
//        verify(authService, times(1)).refreshAccessToken(invalidToken)
//        verify(cookieUtil, times(1)).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME)
//        verify(cookieUtil, never()).addCookie(any(), anyString(), anyString(), anyInt())
//    }
//
//    @Test
//    @DisplayName("토큰 갱신 실패 - 만료된 Refresh Token")
//    fun `refreshToken Fail ExpiredToken`() {
//        // given
//        val expiredToken = "expired.refresh.token"
//
//        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
//            .willReturn(expiredToken)
//
//        given(authService.refreshAccessToken(expiredToken))
//            .willThrow(IllegalArgumentException("Refresh Token이 만료되었습니다."))
//
//        doNothing().`when`(cookieUtil).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME)
//
//        // when
//        val result = authController.refreshToken(request, response)
//
//        // then
//        assertThat(result.statusCode.value()).isEqualTo(401)
//        assertThat(result.body).isNull()
//
//        // 검증: 쿠키 삭제됨
//        verify(cookieUtil, times(1)).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME)
//    }
//
//    @Test
//    @DisplayName("토큰 갱신 실패 - Redis에 저장된 토큰과 불일치")
//    fun `refreshToken Fail TokenMismatch`() {
//        // given
//        val mismatchedToken = "mismatched.refresh.token"
//
//        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
//            .willReturn(mismatchedToken)
//
//        given(authService.refreshAccessToken(mismatchedToken))
//            .willThrow(IllegalArgumentException("Refresh Token이 일치하지 않습니다."))
//
//        doNothing().`when`(cookieUtil).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME)
//
//        // when
//        val result = authController.refreshToken(request, response)
//
//        // then
//        assertThat(result.statusCode.value()).isEqualTo(401)
//
//        // 검증: 불일치한 토큰은 삭제됨
//        verify(cookieUtil, times(1)).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME)
//    }
//
//    // ========== 쿠키 처리 검증 테스트 ==========
//
//    @Test
//    @DisplayName("토큰 갱신 성공 시 새 Refresh Token이 쿠키에 저장됨")
//    fun `refreshToken Success NewTokenSavedToCookie`() {
//        // given
//        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
//            .willReturn(validRefreshToken)
//
//        given(authService.refreshAccessToken(validRefreshToken))
//            .willReturn(mockTokenResponse)
//
//        doNothing().`when`(cookieUtil).addCookie(
//            any(HttpServletResponse::class.java),
//            anyString(),
//            anyString(),
//            anyInt()
//        )
//
//        // when
//        authController.refreshToken(request, response)
//
//        // then - 정확한 파라미터로 쿠키 추가 호출되는지 검증
//        verify(cookieUtil, times(1)).addCookie(
//            eq(response),
//            eq(REFRESH_TOKEN_COOKIE_NAME),
//            eq("new.refresh.token.jwt.string"),
//            eq(REFRESH_TOKEN_EXPIRE_SECONDS)
//        )
//    }
//
//    @Test
//    @DisplayName("토큰 갱신 실패 시 기존 쿠키가 삭제됨")
//    fun `refreshToken Fail OldCookieDeleted`() {
//        // given
//        val invalidToken = "invalid.token"
//
//        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
//            .willReturn(invalidToken)
//
//        given(authService.refreshAccessToken(invalidToken))
//            .willThrow(IllegalArgumentException("유효하지 않은 토큰"))
//
//        doNothing().`when`(cookieUtil).deleteCookie(any(), anyString())
//
//        // when
//        authController.refreshToken(request, response)
//
//        // then - 쿠키 삭제 호출 검증
//        verify(cookieUtil, times(1)).deleteCookie(
//            eq(response),
//            eq(REFRESH_TOKEN_COOKIE_NAME)
//        )
//    }
//
//    // ========== 엣지 케이스 테스트 ==========
//
//    @Test
//    @DisplayName("빈 문자열 Refresh Token - 실패 처리")
//    fun `refreshToken EmptyString`() {
//        // given
//        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
//            .willReturn("")
//
//        given(authService.refreshAccessToken(""))
//            .willThrow(IllegalArgumentException("빈 토큰"))
//
//        // when
//        val result = authController.refreshToken(request, response)
//
//        // then
//        assertThat(result.statusCode.value()).isEqualTo(401)
//        verify(cookieUtil).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME)
//    }
//
//    @Test
//    @DisplayName("AuthService에서 RuntimeException 발생 시 예외 전파됨")
//    fun `refreshToken ServiceException`() {
//        // given
//        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
//            .willReturn(validRefreshToken)
//
//        given(authService.refreshAccessToken(validRefreshToken))
//            .willThrow(RuntimeException("서버 오류"))
//
//        // when & then
//        // RuntimeException은 IllegalArgumentException이 아니므로 예외 전파됨
//        // Controller에서 catch하지 않으므로 예외가 그대로 던져짐
//        assertThrows(RuntimeException::class.java) {
//            authController.refreshToken(request, response)
//        }
//
//        // 쿠키 삭제는 호출되지 않음 (catch가 IllegalArgumentException만 처리)
//        verify(cookieUtil, never()).deleteCookie(any(), anyString())
//        verify(cookieUtil, never()).addCookie(any(), anyString(), anyString(), anyInt())
//    }
}