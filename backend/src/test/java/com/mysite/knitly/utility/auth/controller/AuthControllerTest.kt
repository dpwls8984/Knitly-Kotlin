package com.mysite.knitly.utility.auth.controller;

import com.mysite.knitly.utility.auth.dto.TokenRefreshResponse;
import com.mysite.knitly.utility.auth.service.AuthService;
import com.mysite.knitly.utility.cookie.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * AuthController 단위 테스트
 *
 * 테스트 대상: Access Token 갱신 API
 * - Refresh Token 검증
 * - 새로운 토큰 발급
 * - 쿠키 처리
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthController authController;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_EXPIRE_SECONDS = 604800; // 7일

    private String validRefreshToken;
    private TokenRefreshResponse mockTokenResponse;

    @BeforeEach
    void setUp() {
        // @Value 필드 주입 (ReflectionTestUtils 사용)
        ReflectionTestUtils.setField(authController, "refreshTokenExpireSeconds", REFRESH_TOKEN_EXPIRE_SECONDS);

        // 테스트 데이터 준비
        validRefreshToken = "valid.refresh.token.jwt.string";

        mockTokenResponse = TokenRefreshResponse.of(
                "new.access.token.jwt.string",
                "new.refresh.token.jwt.string",
                1800L  // 30분
        );
    }

    // ========== 토큰 갱신 성공 테스트 ==========

    @Test
    @DisplayName("토큰 갱신 성공 - 유효한 Refresh Token")
    void refreshToken_Success() {
        // given
        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
                .willReturn(Optional.of(validRefreshToken));

        given(authService.refreshAccessToken(validRefreshToken))
                .willReturn(mockTokenResponse);

        doNothing().when(cookieUtil).addCookie(
                any(HttpServletResponse.class),
                eq(REFRESH_TOKEN_COOKIE_NAME),
                anyString(),
                anyInt()
        );

        // when
        ResponseEntity<TokenRefreshResponse> result = authController.refreshToken(request, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getAccessToken()).isEqualTo("new.access.token.jwt.string");
        assertThat(result.getBody().getRefreshToken()).isEqualTo("new.refresh.token.jwt.string");
        assertThat(result.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(result.getBody().getExpiresIn()).isEqualTo(1800L);

        // 검증
        verify(cookieUtil, times(1)).getCookie(request, REFRESH_TOKEN_COOKIE_NAME);
        verify(authService, times(1)).refreshAccessToken(validRefreshToken);
        verify(cookieUtil, times(1)).addCookie(
                response,
                REFRESH_TOKEN_COOKIE_NAME,
                "new.refresh.token.jwt.string",
                REFRESH_TOKEN_EXPIRE_SECONDS
        );
    }

    // ========== 토큰 갱신 실패 테스트 ==========

    @Test
    @DisplayName("토큰 갱신 실패 - Refresh Token이 쿠키에 없음")
    void refreshToken_Fail_NoCookie() {
        // given
        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
                .willReturn(Optional.empty());

        // when
        ResponseEntity<TokenRefreshResponse> result = authController.refreshToken(request, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(400);
        assertThat(result.getBody()).isNull();

        // 검증: authService는 호출되지 않음
        verify(cookieUtil, times(1)).getCookie(request, REFRESH_TOKEN_COOKIE_NAME);
        verify(authService, never()).refreshAccessToken(anyString());
        verify(cookieUtil, never()).addCookie(any(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 Refresh Token")
    void refreshToken_Fail_InvalidToken() {
        // given
        String invalidToken = "invalid.refresh.token";

        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
                .willReturn(Optional.of(invalidToken));

        given(authService.refreshAccessToken(invalidToken))
                .willThrow(new IllegalArgumentException("유효하지 않은 Refresh Token입니다."));

        doNothing().when(cookieUtil).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);

        // when
        ResponseEntity<TokenRefreshResponse> result = authController.refreshToken(request, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(401);
        assertThat(result.getBody()).isNull();

        // 검증: 쿠키 삭제됨
        verify(cookieUtil, times(1)).getCookie(request, REFRESH_TOKEN_COOKIE_NAME);
        verify(authService, times(1)).refreshAccessToken(invalidToken);
        verify(cookieUtil, times(1)).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
        verify(cookieUtil, never()).addCookie(any(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 만료된 Refresh Token")
    void refreshToken_Fail_ExpiredToken() {
        // given
        String expiredToken = "expired.refresh.token";

        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
                .willReturn(Optional.of(expiredToken));

        given(authService.refreshAccessToken(expiredToken))
                .willThrow(new IllegalArgumentException("Refresh Token이 만료되었습니다."));

        doNothing().when(cookieUtil).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);

        // when
        ResponseEntity<TokenRefreshResponse> result = authController.refreshToken(request, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(401);
        assertThat(result.getBody()).isNull();

        // 검증: 쿠키 삭제됨
        verify(cookieUtil, times(1)).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - Redis에 저장된 토큰과 불일치")
    void refreshToken_Fail_TokenMismatch() {
        // given
        String mismatchedToken = "mismatched.refresh.token";

        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
                .willReturn(Optional.of(mismatchedToken));

        given(authService.refreshAccessToken(mismatchedToken))
                .willThrow(new IllegalArgumentException("Refresh Token이 일치하지 않습니다."));

        doNothing().when(cookieUtil).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);

        // when
        ResponseEntity<TokenRefreshResponse> result = authController.refreshToken(request, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(401);

        // 검증: 불일치한 토큰은 삭제됨
        verify(cookieUtil, times(1)).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
    }

    // ========== 쿠키 처리 검증 테스트 ==========

    @Test
    @DisplayName("토큰 갱신 성공 시 새 Refresh Token이 쿠키에 저장됨")
    void refreshToken_Success_NewTokenSavedToCookie() {
        // given
        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
                .willReturn(Optional.of(validRefreshToken));

        given(authService.refreshAccessToken(validRefreshToken))
                .willReturn(mockTokenResponse);

        doNothing().when(cookieUtil).addCookie(
                any(HttpServletResponse.class),
                anyString(),
                anyString(),
                anyInt()
        );

        // when
        authController.refreshToken(request, response);

        // then - 정확한 파라미터로 쿠키 추가 호출되는지 검증
        verify(cookieUtil, times(1)).addCookie(
                eq(response),
                eq(REFRESH_TOKEN_COOKIE_NAME),
                eq("new.refresh.token.jwt.string"),
                eq(REFRESH_TOKEN_EXPIRE_SECONDS)
        );
    }

    @Test
    @DisplayName("토큰 갱신 실패 시 기존 쿠키가 삭제됨")
    void refreshToken_Fail_OldCookieDeleted() {
        // given
        String invalidToken = "invalid.token";

        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
                .willReturn(Optional.of(invalidToken));

        given(authService.refreshAccessToken(invalidToken))
                .willThrow(new IllegalArgumentException("유효하지 않은 토큰"));

        doNothing().when(cookieUtil).deleteCookie(any(), anyString());

        // when
        authController.refreshToken(request, response);

        // then - 쿠키 삭제 호출 검증
        verify(cookieUtil, times(1)).deleteCookie(
                eq(response),
                eq(REFRESH_TOKEN_COOKIE_NAME)
        );
    }

    // ========== 엣지 케이스 테스트 ==========

    @Test
    @DisplayName("빈 문자열 Refresh Token - 실패 처리")
    void refreshToken_EmptyString() {
        // given
        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
                .willReturn(Optional.of(""));

        given(authService.refreshAccessToken(""))
                .willThrow(new IllegalArgumentException("빈 토큰"));

        // when
        ResponseEntity<TokenRefreshResponse> result = authController.refreshToken(request, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(401);
        verify(cookieUtil).deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
    }

    @Test
    @DisplayName("AuthService에서 RuntimeException 발생 시 예외 전파됨")
    void refreshToken_ServiceException() {
        // given
        given(cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME))
                .willReturn(Optional.of(validRefreshToken));

        given(authService.refreshAccessToken(validRefreshToken))
                .willThrow(new RuntimeException("서버 오류"));

        // when & then
        // RuntimeException은 IllegalArgumentException이 아니므로 예외 전파됨
        // Controller에서 catch하지 않으므로 예외가 그대로 던져짐
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            authController.refreshToken(request, response);
        });

        // 쿠키 삭제는 호출되지 않음 (catch가 IllegalArgumentException만 처리)
        verify(cookieUtil, never()).deleteCookie(any(), anyString());
        verify(cookieUtil, never()).addCookie(any(), anyString(), anyString(), anyInt());
    }
}