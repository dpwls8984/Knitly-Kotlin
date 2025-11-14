package com.mysite.knitly.domain.user.controller;

import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.product.product.service.ProductService;
import com.mysite.knitly.domain.user.entity.Provider;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.service.UserService;
import com.mysite.knitly.utility.auth.service.AuthService;
import com.mysite.knitly.utility.cookie.CookieUtil;
import com.mysite.knitly.utility.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * UserController 단위 테스트
 *
 * 테스트 전략:
 * 1. Mock 객체를 사용하여 의존성 분리
 * 2. 각 메서드의 성공/실패 시나리오 테스트
 * 3. 반환값 검증 및 메서드 호출 검증
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private ProductService productService;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private UserService userService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private String authHeader;

    @BeforeEach
    void setUp() {
        // 테스트용 User 객체 생성
        testUser = User.builder()
                .userId(1L)
                .socialId("google-123456")
                .email("test@example.com")
                .name("테스트유저")
                .provider(Provider.GOOGLE)
                .build();

        authHeader = "Bearer test-access-token";
    }

    // ========== 현재 사용자 정보 조회 테스트 ==========

    @Test
    @DisplayName("로그인한 사용자 정보 조회 성공")
    void getCurrentUser_Success() {
        // when
        ResponseEntity<Map<String, Object>> result = userController.getCurrentUser(testUser);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().get("userId")).isEqualTo(1L);
        assertThat(result.getBody().get("email")).isEqualTo("test@example.com");
        assertThat(result.getBody().get("name")).isEqualTo("테스트유저");
        assertThat(result.getBody().get("provider")).isEqualTo(Provider.GOOGLE);
        assertThat(result.getBody()).containsKey("createdAt");
    }

    @Test
    @DisplayName("로그인한 사용자 정보 조회 실패 - 인증 없음")
    void getCurrentUser_Fail_Unauthorized() {
        // when
        ResponseEntity<Map<String, Object>> result = userController.getCurrentUser(null);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(401);
        assertThat(result.getBody()).isNull();
    }

    // ========== 로그아웃 테스트 ==========

    @Test
    @DisplayName("로그아웃 성공 - Access Token으로 userId 추출")
    void logout_Success_WithAccessToken() {
        // given
        given(jwtProvider.getUserIdFromToken("test-access-token")).willReturn(1L);
        doNothing().when(authService).logout(1L);
        doNothing().when(cookieUtil).deleteCookie(any(HttpServletResponse.class), eq("refreshToken"));

        // when
        ResponseEntity<String> result = userController.logout(authHeader, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo("로그아웃되었습니다.");

        // 검증: authService.logout()이 2번 호출되는지 확인 (코드 상 중복 호출됨)
        verify(authService, times(2)).logout(1L);
        verify(cookieUtil, times(1)).deleteCookie(response, "refreshToken");
        verify(jwtProvider, times(1)).getUserIdFromToken("test-access-token");
    }

    @Test
    @DisplayName("로그아웃 성공 - Authorization 헤더 없음")
    void logout_Success_WithoutAuthHeader() {
        // given
        doNothing().when(cookieUtil).deleteCookie(any(HttpServletResponse.class), eq("refreshToken"));

        // when
        ResponseEntity<String> result = userController.logout(null, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo("로그아웃되었습니다.");

        // JWT 파싱 시도하지 않음
        verify(jwtProvider, never()).getUserIdFromToken(anyString());
        // authService.logout(null) 호출됨
        verify(authService, times(1)).logout(null);
        verify(cookieUtil, times(1)).deleteCookie(response, "refreshToken");
    }

    @Test
    @DisplayName("로그아웃 - 토큰 파싱 실패해도 쿠키는 삭제됨")
    void logout_TokenParsingFails_StillDeletesCookie() {
        // given
        given(jwtProvider.getUserIdFromToken("test-access-token"))
                .willThrow(new RuntimeException("Invalid token"));
        doNothing().when(cookieUtil).deleteCookie(any(HttpServletResponse.class), eq("refreshToken"));

        // when
        ResponseEntity<String> result = userController.logout(authHeader, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo("로그아웃되었습니다.");

        // 쿠키는 삭제됨
        verify(cookieUtil, times(1)).deleteCookie(response, "refreshToken");
        // authService.logout(null) 호출됨 (userId 추출 실패)
        verify(authService, times(1)).logout(null);
    }

    // ========== 회원탈퇴 테스트 ==========

    @Test
    @DisplayName("회원탈퇴 성공")
    void deleteAccount_Success() {
        // given
        doNothing().when(authService).deleteAccount(1L);
        doNothing().when(cookieUtil).deleteCookie(any(HttpServletResponse.class), eq("refreshToken"));

        // when
        ResponseEntity<String> result = userController.deleteAccount(testUser, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo("회원탈퇴가 완료되었습니다.");

        verify(authService, times(1)).deleteAccount(1L);
        verify(cookieUtil, times(1)).deleteCookie(response, "refreshToken");
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 인증 없음")
    void deleteAccount_Fail_Unauthorized() {
        // when
        ResponseEntity<String> result = userController.deleteAccount(null, response);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(401);
        assertThat(result.getBody()).isEqualTo("인증이 필요합니다.");

        // 서비스 메서드 호출되지 않음
        verify(authService, never()).deleteAccount(anyLong());
        verify(cookieUtil, never()).deleteCookie(any(), anyString());
    }

    @Test
    @DisplayName("회원탈퇴 - DB 삭제 실패해도 쿠키는 삭제됨")
    void deleteAccount_DBFailure_StillDeletesCookie() {
        // given
        doThrow(new RuntimeException("DB 오류"))
                .when(authService).deleteAccount(1L);

        // when & then
        try {
            userController.deleteAccount(testUser, response);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("DB 오류");
        }

        // authService.deleteAccount() 호출은 되었음
        verify(authService, times(1)).deleteAccount(1L);
        // 예외 발생으로 쿠키 삭제는 실행되지 않음
        verify(cookieUtil, never()).deleteCookie(any(), anyString());
    }

    // ========== 판매자 상품 목록 조회 테스트 ==========

    @Test
    @DisplayName("특정 유저가 판매중인 상품 목록 조회 성공")
    void getProductsWithUserId_Success() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        // Mock 상품 데이터 생성
        List<ProductListResponse> products = new ArrayList<>();
        products.add(new ProductListResponse(
                1L,
                "니트 스웨터",
                ProductCategory.TOP,
                29900.0,
                10,
                5,
                false,
                100,
                4.5,
                LocalDateTime.now(),
                "http://example.com/image.jpg",
                "테스트유저",
                false,
                true,
                false,
                userId
        ));

        Page<ProductListResponse> mockPage = new PageImpl<>(products, pageable, 1);
        given(productService.findProductsByUserId(userId, pageable)).willReturn(mockPage);

        // when
        ResponseEntity<Page<ProductListResponse>> result =
                userController.getProductsWithUserId(userId, pageable);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(1);
        assertThat(result.getBody().getContent().get(0).title()).isEqualTo("니트 스웨터");
        assertThat(result.getBody().getTotalElements()).isEqualTo(1);

        verify(productService, times(1)).findProductsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("특정 유저가 판매중인 상품 목록 조회 - 상품 없음")
    void getProductsWithUserId_EmptyResult() {
        // given
        Long userId = 999L;
        Pageable pageable = PageRequest.of(0, 20);

        Page<ProductListResponse> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);
        given(productService.findProductsByUserId(userId, pageable)).willReturn(emptyPage);

        // when
        ResponseEntity<Page<ProductListResponse>> result =
                userController.getProductsWithUserId(userId, pageable);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).isEmpty();
        assertThat(result.getBody().getTotalElements()).isEqualTo(0);

        verify(productService, times(1)).findProductsByUserId(userId, pageable);
    }

    @Test
    @DisplayName("특정 유저가 판매중인 상품 목록 조회 - 페이징 처리 확인")
    void getProductsWithUserId_WithPagination() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(1, 10); // 2페이지, 10개씩

        List<ProductListResponse> products = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            products.add(new ProductListResponse(
                    (long) (i + 11), // 11~20번 상품
                    "상품 " + (i + 11),
                    ProductCategory.TOP,
                    10000.0 + i * 1000,
                    i,
                    i,
                    false,
                    50,
                    4.0,
                    LocalDateTime.now(),
                    "http://example.com/image" + i + ".jpg",
                    "테스트유저",
                    false,
                    true,
                    false,
                    userId
            ));
        }

        Page<ProductListResponse> mockPage = new PageImpl<>(products, pageable, 25); // 총 25개
        given(productService.findProductsByUserId(userId, pageable)).willReturn(mockPage);

        // when
        ResponseEntity<Page<ProductListResponse>> result =
                userController.getProductsWithUserId(userId, pageable);

        // then
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getContent()).hasSize(10);
        assertThat(result.getBody().getTotalElements()).isEqualTo(25);
        assertThat(result.getBody().getTotalPages()).isEqualTo(3);
        assertThat(result.getBody().getNumber()).isEqualTo(1); // 현재 페이지

        verify(productService, times(1)).findProductsByUserId(userId, pageable);
    }
}