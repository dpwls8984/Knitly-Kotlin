package com.mysite.knitly.utility.auth.service

import com.mysite.knitly.domain.design.repository.DesignRepository
import com.mysite.knitly.domain.product.product.service.ProductService
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.domain.user.service.UserService
import com.mysite.knitly.utility.auth.dto.TokenRefreshResponse
import com.mysite.knitly.utility.jwt.JwtProperties
import com.mysite.knitly.utility.jwt.JwtProvider
import com.mysite.knitly.utility.redis.RefreshTokenService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val jwtProvider: JwtProvider,
    private val refreshTokenService: RefreshTokenService,
    private val jwtProperties: JwtProperties,
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val productService: ProductService,
    private val designRepository: DesignRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Refresh Token으로 Access Token 갱신
     */
    fun refreshAccessToken(refreshToken: String): TokenRefreshResponse {
        // 1. Refresh Token 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            log.info("[AuthService] 유효하지 않은 Refresh Token입니다.")
            throw IllegalArgumentException("[AuthService] 유효하지 않은 Refresh Token입니다.")
        }

        // 2. Refresh Token에서 userId 추출
        val userId = jwtProvider.getUserIdFromToken(refreshToken)
        log.info("[AuthService] Token refresh requested - userId: {}", userId)

        // 3. Redis에 저장된 Refresh Token과 비교
        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            throw IllegalArgumentException("[AuthService] Refresh Token이 일치하지 않습니다.")
        }

        // 4. 새로운 Access Token 생성
        val newAccessToken = jwtProvider.createAccessToken(userId)

        // 5. 새로운 Refresh Token 생성 (RTR - Refresh Token Rotation)
        val newRefreshToken = jwtProvider.createRefreshToken(userId)

        // 6. 새로운 Refresh Token을 Redis에 저장 (기존 토큰 덮어쓰기)
        refreshTokenService.saveRefreshToken(userId, newRefreshToken)

        log.info("[AuthService] Token refreshed successfully - userId: {}", userId)

        return TokenRefreshResponse.of(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = jwtProperties.accessTokenExpireSeconds
        )
    }

    /**
     * 로그아웃
     */
    fun logout(userId: Long) {
        refreshTokenService.deleteRefreshToken(userId)
        log.info("[AuthService] User logged out - userId: {}", userId)
    }

    /**
     * 회원탈퇴
     * 1. Refresh Token 삭제 (Redis)
     * 2. User 삭제 (DB)
     */
    @Transactional
    fun deleteAccount(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("[AuthService] 사용자를 찾을 수 없습니다.") }

        log.info("[AuthService] 회원 탈퇴 시작 - userId: {}", userId)

        // 1. Redis에서 Refresh Token 삭제
        refreshTokenService.deleteRefreshToken(userId)
        log.info("[AuthService] Redis에서 Refresh Token 삭제 완료 - userId: {}", userId)

        // 2. 사용자 삭제 (Cascade로 자동으로 연관 데이터 모두 삭제됨!)
        userRepository.delete(user)
        log.info("[AuthService] 사용자 및 모든 연관 데이터 삭제 완료 - userId: {}", userId)
    }
}