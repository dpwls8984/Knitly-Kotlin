package com.mysite.knitly.utility.oauth;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OAuth2UserInfo {
    private String email;
    private String name;
    private String providerId;  // Google의 고유 ID

    /**
     * Google OAuth2 응답에서 사용자 정보 추출
     */
    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        // Google OAuth2 응답 처리
        if ("google".equals(registrationId)) {
            return ofGoogle(attributes);
        }

        throw new IllegalArgumentException("Unsupported provider: " + registrationId);
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
    private static OAuth2UserInfo ofGoogle(Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
                .providerId((String) attributes.get("sub"))
                .email((String) attributes.get("email"))
                .name((String) attributes.get("name"))
                .build();
    }
}
