package com.mysite.knitly.utility.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * HTTP Cookie 유틸리티 클래스
 * - HTTP-only 쿠키 생성, 조회, 삭제 기능 제공
 */
@Slf4j
@Component
public class CookieUtil {

    /**
     * HTTP-only 쿠키 생성
     *
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 만료 시간 (초 단위)
     * @return Cookie 객체
     */
    public Cookie createCookie(String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);  // JavaScript 접근 차단
        cookie.setPath("/");       // 모든 경로에서 접근 가능
        cookie.setMaxAge(maxAge);  // 만료 시간 설정

        // HTTPS 환경에서만 전송 (개발 환경에서는 false)
        // 프로덕션에서는 true로 변경 필요
        cookie.setSecure(false);

        // TODO : 지금은 알아만 둘 것 (지금 중요한 내용은 아님)
        // 안전한 요청에만 쿠키 전송 (개발 환경 권장)
        // None: 모든 크로스 도메인 요청에 쿠키 전송 (Secure=true 필수)
        // 참고: Cookie 객체는 SameSite를 직접 지원하지 않으므로
        // ResponseCookie를 사용하거나 Set-Cookie 헤더를 직접 작성해야 함
        // 예시)

        log.debug("Cookie created - name: {}, maxAge: {} seconds", name, maxAge);
        return cookie;
    }

    /**
     * HTTP-only 쿠키를 응답에 추가
     *
     * @param response HttpServletResponse
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 만료 시간 (초 단위)
     */
    public void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = createCookie(name, value, maxAge);
        response.addCookie(cookie);
        log.info("Cookie added to response - name: {}", name);
    }

    /**
     * 요청에서 특정 쿠키 값 조회
     *
     * @param request HttpServletRequest
     * @param name 쿠키 이름
     * @return Optional<String> 쿠키 값 (없으면 empty)
     */
    public Optional<String> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            log.debug("No cookies found in request");
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /**
     * 쿠키 삭제 (MaxAge를 0으로 설정)
     *
     * @param response HttpServletResponse
     * @param name 삭제할 쿠키 이름
     */
    public void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 즉시 만료
        cookie.setSecure(false);

        response.addCookie(cookie);
        log.info("Cookie deleted - name: {}", name);
    }
}