package com.mysite.knitly.utility.jwt

import com.mysite.knitly.domain.user.service.UserService
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userService: UserService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // 1. 요청 헤더에서 JWT 토큰 추출
            val token = extractTokenFromRequest(request)

            log.info("===> JWT Filter: token = {}", if (token != null) "EXISTS" else "NULL")

            if (token != null && jwtProvider.validateToken(token)) {
                log.info("===> JWT Valid!")

                // 2. 토큰에서 userId 추출
                val userId = jwtProvider.getUserIdFromToken(token)

                // 3. userId로 사용자 조회
                val user = userService.findById(userId)

                // 4. Spring Security 인증 객체 생성
                val authentication = UsernamePasswordAuthenticationToken(
                    user,           // principal
                    null,           // credentials
                    emptyList()     // authorities
                )

                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                // 5. SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().authentication = authentication

                log.debug("JWT authenticated - userId: {}", userId)
            } else {
                log.warn("===> JWT Invalid or null!")
            }

        } catch (e: Exception) {
            log.error("JWT authentication failed: {}", e.message)
            // 인증 실패해도 다음 필터로 진행 (Spring Security가 처리)
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response)
    }

    /**
     * 요청 헤더에서 Bearer 토큰 추출
     * Authorization: Bearer {token}
     */
    private fun extractTokenFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")

        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7) // "Bearer " 제거
        } else {
            null
        }
    }
}