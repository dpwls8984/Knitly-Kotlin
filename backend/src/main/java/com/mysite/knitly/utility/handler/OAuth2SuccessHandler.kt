package com.mysite.knitly.utility.handler

import com.mysite.knitly.domain.user.service.UserService
import com.mysite.knitly.utility.cookie.CookieUtil
import com.mysite.knitly.utility.jwt.JwtProvider
import com.mysite.knitly.utility.oauth.OAuth2UserInfo
import com.mysite.knitly.utility.redis.RefreshTokenService
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class OAuth2SuccessHandler(
    private val userService: UserService,
    private val jwtProvider: JwtProvider,
    private val refreshTokenService: RefreshTokenService,
    private val cookieUtil: CookieUtil
) : SimpleUrlAuthenticationSuccessHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    // TODO : yml 파일, env 파일 수정할것
    // 프론트엔드 URL 설정 (환경변수로 관리 권장)
    @Value("\${frontend.url}")  // 기본값: localhost:3000
    private lateinit var frontendUrl: String

    @Value("\${custom.jwt.refreshTokenExpireSeconds}")
    private val refreshTokenExpireSeconds: Int = 0

    companion object {
        // Refresh Token 쿠키 이름 상수
        private const val REFRESH_TOKEN_COOKIE_NAME = "refreshToken"
    }

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        // 1. OAuth2User 정보 가져오기
        val oAuth2User = authentication.principal as OAuth2User
        val attributes = oAuth2User.attributes

        // 2. 사용자 정보 추출
        val userInfo = OAuth2UserInfo.of("google", attributes)

        log.info("=== OAuth2 Login Success ===")
        log.info("Email: {}", userInfo.email)
        log.info("Name: {}", userInfo.name)
        log.info("Provider ID: {}", userInfo.providerId)

        // 3. 사용자 저장 또는 조회
        val user = userService.processGoogleUser(
            userInfo.providerId,
            userInfo.email,
            userInfo.name
        )

        log.info("User processed - userId: {}", user.userId)

        // 스토어 중복 생성 방지
        userService.ensureUserStore(user)

        // 4. JWT 토큰 발급
        val tokens = jwtProvider.createTokens(user.userId)

        log.info("=== JWT Tokens Created ===")
        log.info("Access Token: {}", tokens.accessToken)
        log.info("Refresh Token: {}", tokens.refreshToken)
        log.info("Expires In: {} seconds", tokens.expiresIn)

        // 5. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(user.userId, tokens.refreshToken)
        log.info("Refresh Token saved to Redis")

        // 6. Refresh Token을 HTTP-only 쿠키에 저장
        cookieUtil.addCookie(
            response,
            REFRESH_TOKEN_COOKIE_NAME,
            tokens.refreshToken,
            refreshTokenExpireSeconds
        )
        log.info("Refresh Token saved to HTTP-only cookie")

        // 7. 프론트엔드로 리다이렉트 (Access Token 포함)
        // 변경: localhost:8080 → frontend.url (localhost:3000)
        val targetUrl = String.format(
            "%s?userId=%s&email=%s&name=%s&accessToken=%s",
            frontendUrl,
            user.userId,
            URLEncoder.encode(user.email, StandardCharsets.UTF_8),
            URLEncoder.encode(user.name, StandardCharsets.UTF_8),
            tokens.accessToken
        )

        log.info("Redirecting to: {}", targetUrl)
        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}