package com.mysite.knitly.utility.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class JsonAccessDeniedHandler : AccessDeniedHandler {

    @Throws(IOException::class)
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        response.status = HttpServletResponse.SC_FORBIDDEN // 403
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write("""
            {"error":{"code":"AUTH_FORBIDDEN","status":403,"message":"접근 권한이 없습니다."}}
        """.trimIndent())
    }
}