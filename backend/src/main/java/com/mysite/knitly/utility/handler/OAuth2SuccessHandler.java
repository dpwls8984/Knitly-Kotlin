package com.mysite.knitly.utility.handler;

import com.mysite.knitly.utility.oauth.OAuth2UserInfo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 1. OAuth2User 정보 가져오기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 2. 사용자 정보 추출
        OAuth2UserInfo userInfo = OAuth2UserInfo.of("google", attributes);

        log.info("=== OAuth2 Login Success ===");
        log.info("Email: {}", userInfo.getEmail());
        log.info("Name: {}", userInfo.getName());
        log.info("Provider ID: {}", userInfo.getProviderId());

        // 3. TODO: 다음 단계에서 구현
        // - DB에 사용자 저장 (신규 사용자인 경우)
        // - JWT 토큰 발급
        // - Refresh Token 발급 및 Redis 저장

        // 4. 임시 리다이렉트 (테스트용)
        String targetUrl = "http://localhost:8080/login/success?email=" + userInfo.getEmail();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
