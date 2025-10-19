package com.mysite.knitly.utility.redis;

import com.mysite.knitly.utility.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    private static final String REFRESH_TOKEN_PREFIX = "RT:";

    /**
     * Refresh Token을 Redis에 저장
     * Key: RT:{userId}
     * Value: refreshToken
     * TTL: 7일
     */
    public void saveRefreshToken(UUID userId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + userId.toString();

        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                jwtProperties.getRefreshTokenExpireSeconds(),
                TimeUnit.SECONDS
        );

        log.info("Refresh Token saved to Redis - userId: {}", userId);
    }

    /**
     * Redis에서 Refresh Token 조회
     */
    public String getRefreshToken(UUID userId) {
        String key = REFRESH_TOKEN_PREFIX + userId.toString();
        String refreshToken = redisTemplate.opsForValue().get(key);

        if (refreshToken == null) {
            log.warn("Refresh Token not found in Redis - userId: {}", userId);
        }

        return refreshToken;
    }

    /**
     * Refresh Token 검증
     * - Redis에 저장된 토큰과 일치하는지 확인
     */
    public boolean validateRefreshToken(UUID userId, String refreshToken) {
        String storedToken = getRefreshToken(userId);

        if (storedToken == null) {
            log.warn("No stored refresh token for userId: {}", userId);
            return false;
        }

        boolean isValid = storedToken.equals(refreshToken);

        if (!isValid) {
            log.warn("Refresh token mismatch for userId: {}", userId);
        }

        return isValid;
    }

    /**
     * Refresh Token 삭제 (로그아웃 시 사용)
     */
    public void deleteRefreshToken(UUID userId) {
        String key = REFRESH_TOKEN_PREFIX + userId.toString();
        Boolean deleted = redisTemplate.delete(key);

        if (Boolean.TRUE.equals(deleted)) {
            log.info("Refresh Token deleted from Redis - userId: {}", userId);
        } else {
            log.warn("Refresh Token not found for deletion - userId: {}", userId);
        }
    }

    /**
     * Refresh Token 존재 여부 확인
     */
    public boolean existsRefreshToken(UUID userId) {
        String key = REFRESH_TOKEN_PREFIX + userId.toString();
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
