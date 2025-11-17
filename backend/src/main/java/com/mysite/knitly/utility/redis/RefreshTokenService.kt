package com.mysite.knitly.utility.redis

import com.mysite.knitly.utility.jwt.JwtProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RefreshTokenService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val jwtProperties: JwtProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val REFRESH_TOKEN_PREFIX = "RT:"
    }

    /**
     * Refresh Token을 Redis에 저장
     * Key: RT:{userId}
     * Value: refreshToken
     * TTL: 7일
     */
    fun saveRefreshToken(userId: Long, refreshToken: String) {
        val key = "$REFRESH_TOKEN_PREFIX$userId"

        redisTemplate.opsForValue().set(
            key,
            refreshToken,
            jwtProperties.refreshTokenExpireSeconds,
            TimeUnit.SECONDS
        )

        log.info("==> Refresh Token saved to Redis - userId: {}", userId)
        log.info("==> Refresh Token saved to Redis - refreshToken: {}", refreshToken)
    }

    /**
     * Redis에서 Refresh Token 조회
     */
    fun getRefreshToken(userId: Long): String? {
        val key = "$REFRESH_TOKEN_PREFIX$userId"
        val refreshToken = redisTemplate.opsForValue().get(key)

        if (refreshToken == null) {
            log.warn("Refresh Token not found in Redis - userId: {}", userId)
        }

        return refreshToken
    }

    /**
     * Refresh Token 검증
     * - Redis에 저장된 토큰과 일치하는지 확인
     */
    fun validateRefreshToken(userId: Long, refreshToken: String): Boolean {
        val storedToken = getRefreshToken(userId)

        if (storedToken == null) {
            log.warn("No stored refresh token for userId: {}", userId)
            return false
        }

        val isValid = storedToken == refreshToken

        if (!isValid) {
            log.warn("Refresh token mismatch for userId: {}", userId)
        }

        return isValid
    }

    /**
     * Refresh Token 삭제 (로그아웃 시 사용)
     */
    fun deleteRefreshToken(userId: Long) {
        val key = "$REFRESH_TOKEN_PREFIX$userId"
        val deleted = redisTemplate.delete(key)

        if (deleted == true) {
            log.info("Refresh Token deleted from Redis - userId: {}", userId)
        } else {
            log.warn("Refresh Token not found for deletion - userId: {}", userId)
        }
    }
}