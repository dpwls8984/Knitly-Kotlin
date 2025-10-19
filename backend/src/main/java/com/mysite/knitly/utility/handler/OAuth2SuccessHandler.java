package com.mysite.knitly.utility.handler;

import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.service.UserService;
import com.mysite.knitly.utility.oauth.OAuth2UserInfo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;

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

        // 3. 사용자 저장 또는 조회 (userService 사용!)
        User user = userService.processGoogleUser(
                userInfo.getProviderId(),
                userInfo.getEmail(),
                userInfo.getName()
        );

        log.info("User processed - userId: {}", user.getUserId());

        // - JWT 토큰 발급
        // - Refresh Token 발급 및 Redis 저장

        // 5. 임시 리다이렉트 (테스트용)
        String targetUrl = String.format(
                "http://localhost:8080/login/success?userId=%s&email=%s&name=%s",
                user.getUserId(),
                URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8),
                URLEncoder.encode(user.getName(), StandardCharsets.UTF_8)  // "홍길동" → "%ED%99%8D%EA%B8%B8%EB%8F%99" // 한글이름이 안나오는 오류 해결
        );
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
