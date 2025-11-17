package com.mysite.knitly.utility.auth.dto

data class TokenRefreshResponse(
        val accessToken: String,
        val refreshToken: String,
        val tokenType: String = "Bearer",
        val expiresIn: Long
) {
    companion object {
        fun of(accessToken: String, refreshToken: String, expiresIn: Long): TokenRefreshResponse {
            return TokenRefreshResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = expiresIn
            )
        }
    }
}