package com.mysite.knitly.utility.jwt;

import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. 요청 헤더에서 JWT 토큰 추출
            String token = extractTokenFromRequest(request);

            if (token != null && jwtProvider.validateToken(token)) {
                // 2. 토큰에서 userId 추출
                Long userId = jwtProvider.getUserIdFromToken(token);

                // 3. userId로 사용자 조회
                User user = userService.findById(userId);

                // 4. Spring Security 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,                    // principal
                                null,                    // credentials
                                Collections.emptyList()  // authorities
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 5. SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT authenticated - userId: {}", userId);
            }

        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            // 인증 실패해도 다음 필터로 진행 (Spring Security가 처리)
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더에서 Bearer 토큰 추출
     * Authorization: Bearer {token}
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }

        return null;
    }
}
