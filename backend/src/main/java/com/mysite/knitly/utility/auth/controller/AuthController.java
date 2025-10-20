package com.mysite.knitly.utility.auth.controller;

import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.utility.auth.dto.TokenRefreshRequest;
import com.mysite.knitly.utility.auth.dto.TokenRefreshResponse;
import com.mysite.knitly.utility.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Access Token 갱신 API
     * POST /api/auth/refresh
     */
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
     * 로그아웃 API
     * POST /api/auth/logout
     * JWT 인증 필요
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }

        log.info("Logout API called - userId: {}", user.getUserId());

        authService.logout(user.getUserId());

        return ResponseEntity.ok("로그아웃되었습니다.");
    }

    /**
     * 테스트용 엔드포인트
     * GET /api/auth/test
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth API is working!");
    }
}
