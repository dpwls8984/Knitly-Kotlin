package com.mysite.knitly.utility.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        log.error("=== OAuth2 Login Failed ===");
        log.error("Exception Type: {}", exception.getClass().getName());
        log.error("Error Message: {}", exception.getMessage());

        String errorMessage = "로그인에 실패했습니다.";

        // OAuth2 관련 에러인 경우 상세 정보 출력
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            OAuth2Error error = oauth2Exception.getError();

            log.error("OAuth2 Error Code: {}", error.getErrorCode());
            log.error("OAuth2 Error Description: {}", error.getDescription());

            errorMessage = error.getDescription() != null ? error.getDescription() : errorMessage;
        }

        // 스택 트레이스 출력 (개발 환경용)
        log.error("Stack Trace: ", exception);

        // TODO : 프론트페이지로 리다이랙트할것(수정해야함)
        // 에러 페이지로 리다이렉트
        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        //String targetUrl = "http://localhost:8080/login/error?message=" + encodedMessage;

        // 프론트엔드로 리다이렉트 (에러 파라미터 포함)
        //String targetUrl = String.format("%s/?error=%s", frontendUrl, encodedMessage);

        // 프론트 메인으로 리디랙션
        String targetUrl = frontendUrl + "/?loginError=true";

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
