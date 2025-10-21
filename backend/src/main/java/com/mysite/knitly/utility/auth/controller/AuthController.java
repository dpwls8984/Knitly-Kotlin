package com.mysite.knitly.utility.auth.controller;

import com.mysite.knitly.utility.auth.dto.TokenRefreshRequest;
import com.mysite.knitly.utility.auth.dto.TokenRefreshResponse;
import com.mysite.knitly.utility.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Access Token 갱신 API
     * POST /api/auth/refresh
     */
    @Operation(
            summary = "토큰 갱신",
            description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다."
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
                        "expiresIn": 900
                    }
                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 토큰 또는 만료된 토큰"
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        log.info("Token refresh API called");

        try {
            TokenRefreshResponse response = authService.refreshAccessToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
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
