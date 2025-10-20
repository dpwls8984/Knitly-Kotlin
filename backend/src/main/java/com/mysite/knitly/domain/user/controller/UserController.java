package com.mysite.knitly.domain.user.controller;

import com.mysite.knitly.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    /**
     * 현재 로그인한 사용자 정보 조회 (JWT 인증 필요)
     * GET /api/user/me
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal User user) {

        if (user == null) {
            log.warn("User is null in /api/user/me");
            return ResponseEntity.status(401).build();
        }

        log.info("User info requested - userId: {}", user.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUserId());
        response.put("email", user.getEmail());
        response.put("name", user.getName());
        response.put("provider", user.getProvider());
        response.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * 테스트용 보호된 엔드포인트
     * GET /api/user/protected
     */
    @GetMapping("/protected")
    public ResponseEntity<String> protectedEndpoint(@AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }

        return ResponseEntity.ok("안녕하세요, " + user.getName() + "님! 이것은 보호된 리소스입니다.");
    }
}
