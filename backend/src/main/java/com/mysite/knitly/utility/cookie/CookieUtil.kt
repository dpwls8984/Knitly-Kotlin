package com.mysite.knitly.utility.cookie

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * HTTP Cookie 유틸리티 클래스
 * - HTTP-only 쿠키 생성, 조회, 삭제 기능 제공
 */
@Component
class CookieUtil {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * HTTP-only 쿠키 생성
     *
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 만료 시간 (초 단위)
     * @return Cookie 객체
     */
    fun createCookie(name: String, value: String, maxAge: Int): Cookie {
        return Cookie(name, value).apply {
            isHttpOnly = true  // JavaScript 접근 차단
            path = "/"         // 모든 경로에서 접근 가능
            this.maxAge = maxAge  // 만료 시간 설정

            // HTTPS 환경에서만 전송 (개발 환경에서는 false)
            // 프로덕션에서는 true로 변경 필요
            secure = false

            // TODO : 지금은 알아만 둘 것 (지금 중요한 내용은 아님)
            // 안전한 요청에만 쿠키 전송 (개발 환경 권장)
            // None: 모든 크로스 도메인 요청에 쿠키 전송 (Secure=true 필수)
        }.also {
            log.debug("Cookie created - name: {}, maxAge: {} seconds", name, maxAge)
        }
    }

    /**
     * HTTP-only 쿠키를 응답에 추가
     *
     * @param response HttpServletResponse
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 만료 시간 (초 단위)
     */
    fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        val cookie = createCookie(name, value, maxAge)
        response.addCookie(cookie)
        log.info("Cookie added to response - name: {}", name)
    }

    /**
     * 요청에서 특정 쿠키 값 조회
     *
     * @param request HttpServletRequest
     * @param name 쿠키 이름
     * @return 쿠키 값 (없으면 null)
     */
    fun getCookie(request: HttpServletRequest, name: String): String? {
        val cookies = request.cookies

        if (cookies == null) {
            log.debug("No cookies found in request")
            return null
        }

        return cookies
            .firstOrNull { it.name == name }
            ?.value
    }

    /**
     * 쿠키 삭제 (MaxAge를 0으로 설정)
     *
     * @param response HttpServletResponse
     * @param name 삭제할 쿠키 이름
     */
    fun deleteCookie(response: HttpServletResponse, name: String) {
        val cookie = Cookie(name, null).apply {
            isHttpOnly = true
            path = "/"
            maxAge = 0  // 즉시 만료
            secure = false
        }

        response.addCookie(cookie)
        log.info("Cookie deleted - name: {}", name)
    }
}