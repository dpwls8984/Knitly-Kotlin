package com.mysite.knitly.utility.auth.controller;

import com.mysite.knitly.utility.auth.dto.TokenRefreshResponse;
import com.mysite.knitly.utility.auth.service.AuthService;
import com.mysite.knitly.utility.cookie.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @Value("${custom.jwt.refreshTokenExpireSeconds}")
    private int refreshTokenExpireSeconds;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    /**
     * Access Token 갱신 API
     * POST /api/auth/refresh
     *
     * 쿠키에서 Refresh Token을 읽어 새로운 Access Token과 Refresh Token을 발급
     */
    @Operation(
            summary = "토큰 갱신",
            description = "HTTP-only 쿠키의 Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "갱신 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
                        "tokenType": "Bearer",
                        "expiresIn": 1800
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh Token이 없거나 유효하지 않음"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "만료되었거나 유효하지 않은 토큰"
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("Token refresh API called");

        // 1. 쿠키에서 Refresh Token 가져오기
        String refreshToken = cookieUtil.getCookie(request, REFRESH_TOKEN_COOKIE_NAME)
                .orElse(null);

        if (refreshToken == null) {
            log.error("Refresh Token not found in cookie");
            return ResponseEntity.badRequest().build();
        }

        log.debug("Refresh Token found in cookie: {}...", refreshToken.substring(0, 20));

        try {
            // 2. 새로운 토큰 발급
            TokenRefreshResponse tokenResponse = authService.refreshAccessToken(refreshToken);

            log.info("New tokens created successfully");
            log.debug("New Access Token: {}", tokenResponse.getAccessToken());
            log.debug("New Refresh Token: {}", tokenResponse.getRefreshToken());

            // 3. 새로운 Refresh Token을 쿠키에 저장
            cookieUtil.addCookie(
                    response,
                    REFRESH_TOKEN_COOKIE_NAME,
                    tokenResponse.getRefreshToken(),
                    refreshTokenExpireSeconds
            );

            log.info("New Refresh Token saved to cookie");

            return ResponseEntity.ok(tokenResponse);

        } catch (IllegalArgumentException e) {
            log.error("Token refresh failed: {}", e.getMessage());

            // 실패 시 쿠키 삭제
            cookieUtil.deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
            log.info("Invalid Refresh Token removed from cookie");

            return ResponseEntity.status(401).build();
        }
    }

    /**
     * 테스트용 엔드포인트
     * GET /api/auth/test
     */
    @Operation(
            summary = "API 테스트",
            description = "Auth API가 정상 작동하는지 확인하는 테스트 엔드포인트입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "성공",
            content = @Content(
                    mediaType = "text/plain",
                    examples = @ExampleObject(value = "Auth API is working!")
            )
    )
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth API is working!");
    }
}