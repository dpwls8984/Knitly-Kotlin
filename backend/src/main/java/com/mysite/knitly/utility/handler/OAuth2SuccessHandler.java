package com.mysite.knitly.utility.handler;

import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.service.UserService;
import com.mysite.knitly.utility.jwt.JwtProvider;
import com.mysite.knitly.utility.jwt.TokenResponse;
import com.mysite.knitly.utility.oauth.OAuth2UserInfo;
import com.mysite.knitly.utility.redis.RefreshTokenService;
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
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

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

        // 4. JWT 토큰 발급
        TokenResponse tokens = jwtProvider.createTokens(user.getUserId());

        log.info("=== JWT Tokens Created ===");
        log.info("Access Token: {}...", tokens.getAccessToken().substring(0, 20));
        log.info("Refresh Token: {}...", tokens.getRefreshToken().substring(0, 20));
        log.info("Expires In: {} seconds", tokens.getExpiresIn());

        // 5. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(user.getUserId(), tokens.getRefreshToken());
        log.info("Refresh Token saved to Redis");

        // 6. 임시 리다이렉트 (테스트용) - 토큰 정보 포함
        String targetUrl = String.format(
                "http://localhost:8080/login/success?userId=%s&email=%s&name=%s&accessToken=%s",
                user.getUserId(),
                URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8),
                URLEncoder.encode(user.getName(), StandardCharsets.UTF_8),
                tokens.getAccessToken()
        );
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}