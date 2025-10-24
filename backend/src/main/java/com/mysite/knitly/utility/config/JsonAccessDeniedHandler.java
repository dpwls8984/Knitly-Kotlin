package com.mysite.knitly.utility.config;

import jakarta.servlet.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res,
                       AccessDeniedException ex) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("""
        {"error":{"code":"AUTH_FORBIDDEN","status":403,"message":"접근 권한이 없습니다."}}
        """);
    }
}
