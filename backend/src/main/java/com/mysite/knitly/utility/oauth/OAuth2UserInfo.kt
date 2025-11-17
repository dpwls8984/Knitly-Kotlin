package com.mysite.knitly.utility.oauth

data class OAuth2UserInfo(
        val email: String,
        val name: String,
        val providerId: String  // Google의 고유 ID
) {
    companion object {
        /**
         * Google OAuth2 응답에서 사용자 정보 추출
         */
        fun of(registrationId: String, attributes: Map<String, Any>): OAuth2UserInfo {
            return when (registrationId.lowercase()) {
                "google" -> ofGoogle(attributes)
                else -> throw IllegalArgumentException("Unsupported provider: $registrationId")
            }
        }

        /**
         * Google 응답 파싱
         * Google 응답 예시:
         * {
         *   "sub": "1234567890",
         *   "name": "홍길동",
         *   "email": "user@gmail.com",
         *   "picture": "https://..."
         * }
         */
        private fun ofGoogle(attributes: Map<String, Any>): OAuth2UserInfo {
            return OAuth2UserInfo(
                    providerId = attributes["sub"] as String,
                    email = attributes["email"] as String,
                    name = attributes["name"] as String
            )
        }
    }
}