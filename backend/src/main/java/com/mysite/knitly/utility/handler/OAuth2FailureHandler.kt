package com.mysite.knitly.utility.handler

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class OAuth2FailureHandler : SimpleUrlAuthenticationFailureHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${frontend.url}")
    private lateinit var frontendUrl: String

    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        log.error("=== OAuth2 Login Failed ===")
        log.error("Exception Type: {}", exception.javaClass.name)
        log.error("Error Message: {}", exception.message)

        var errorMessage = "로그인에 실패했습니다."

        // OAuth2 관련 에러인 경우 상세 정보 출력
        if (exception is OAuth2AuthenticationException) {
            val error = exception.error

            log.error("OAuth2 Error Code: {}", error.errorCode)
            log.error("OAuth2 Error Description: {}", error.description)

            errorMessage = error.description ?: errorMessage
        }

        // 스택 트레이스 출력 (개발 환경용)
        log.error("Stack Trace: ", exception)

        // 에러 페이지로 리다이렉트
        val encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)

        // 프론트 메인으로 리디렉션
        val targetUrl = "$frontendUrl/?loginError=true"

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}