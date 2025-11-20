package com.mysite.knitly.utility.jwt

data class TokenResponse(
        val accessToken: String,
        val refreshToken: String,
        val tokenType: String = "Bearer",
        val expiresIn: Long  // Access Token 만료 시간 (초)
) {
    companion object {
        fun of(accessToken: String, refreshToken: String, expiresIn: Long): TokenResponse {
            return TokenResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = expiresIn
            )
        }
    }
}