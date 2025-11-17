package com.mysite.knitly.utility.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class JsonAuthEntryPoint : AuthenticationEntryPoint {

    @Throws(IOException::class)
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED // 401
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write("""
            {"error":{"code":"AUTH_UNAUTHORIZED","status":401,"message":"로그인이 필요합니다."}}
        """.trimIndent())
    }
}