package com.mysite.knitly.utility.auth.service;

import com.mysite.knitly.domain.user.service.UserService;
import com.mysite.knitly.utility.auth.dto.TokenRefreshResponse;
import com.mysite.knitly.utility.jwt.JwtProperties;
import com.mysite.knitly.utility.jwt.JwtProvider;
import com.mysite.knitly.utility.redis.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final UserService userService;

    /**
     * Refresh Token으로 Access Token 갱신
     */
    public TokenRefreshResponse refreshAccessToken(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            log.info("유효하지 않은 Refresh Token입니다.");
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. Refresh Token에서 userId 추출
        Long userId = jwtProvider.getUserIdFromToken(refreshToken);
        log.info("Token refresh requested - userId: {}", userId);

        // 3. Redis에 저장된 Refresh Token과 비교
        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 일치하지 않습니다.");
        }

        // 4. 새로운 Access Token 생성
        String newAccessToken = jwtProvider.createAccessToken(userId);

        // 5. 새로운 Refresh Token 생성 (RTR - Refresh Token Rotation)
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        // 6. 새로운 Refresh Token을 Redis에 저장 (기존 토큰 덮어쓰기)
        refreshTokenService.saveRefreshToken(userId, newRefreshToken);

        log.info("Token refreshed successfully - userId: {}", userId);

        return TokenRefreshResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getAccessTokenExpireSeconds()
        );
    }

    /**
     * 로그아웃
     */
    public void logout(Long userId) {
        refreshTokenService.deleteRefreshToken(userId);
        log.info("User logged out - userId: {}", userId);
    }

    /**
     * 회원탈퇴
     * 1. Refresh Token 삭제 (Redis)
     * 2. User 삭제 (DB)
     */
    @Transactional
    public void deleteAccount(Long userId) {
        // 1. Redis에서 Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(userId);

        // 2. DB에서 사용자 삭제
        userService.deleteUser(userId);

        log.info("Account deleted - userId: {}", userId);
    }
}
