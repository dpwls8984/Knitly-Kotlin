package com.mysite.knitly.utility.jwt

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * SecretKey 생성
     */
    private fun getSigningKey(): SecretKey {
        val keyBytes = jwtProperties.secretKey.toByteArray(StandardCharsets.UTF_8)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    /**
     * Access Token 생성
     */
    fun createAccessToken(userId: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.accessTokenExpireSeconds * 1000)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact()
    }

    /**
     * Refresh Token 생성
     */
    fun createRefreshToken(userId: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtProperties.refreshTokenExpireSeconds * 1000)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact()
    }

    /**
     * 토큰에서 userId 추출
     */
    fun getUserIdFromToken(token: String): Long {
        val claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload

        return claims.subject.toLong()
    }

    /**
     * 토큰 유효성 검증
     */
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: SignatureException) {
            log.error("잘못된 JWT 서명입니다.")
            false
        } catch (e: MalformedJwtException) {
            log.error("잘못된 JWT 서명입니다.")
            false
        } catch (e: ExpiredJwtException) {
            log.error("만료된 JWT 토큰입니다.")
            false
        } catch (e: UnsupportedJwtException) {
            log.error("지원되지 않는 JWT 토큰입니다.")
            false
        } catch (e: IllegalArgumentException) {
            log.error("JWT 토큰이 잘못되었습니다.")
            false
        }
    }

    /**
     * Access Token과 Refresh Token을 함께 생성
     */
    fun createTokens(userId: Long): TokenResponse {
        val accessToken = createAccessToken(userId)
        val refreshToken = createRefreshToken(userId)

        return TokenResponse.of(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtProperties.accessTokenExpireSeconds
        )
    }
}