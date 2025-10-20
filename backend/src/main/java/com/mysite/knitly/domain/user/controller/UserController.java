package com.mysite.knitly.domain.user.controller;

import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.utility.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    /**
     * 현재 로그인한 사용자 정보 조회 (JWT 인증 필요)
     * GET /users/me
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
     * 로그아웃
     * POST /users/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }

        log.info("Logout requested - userId: {}", user.getUserId());

        authService.logout(user.getUserId());

        return ResponseEntity.ok("로그아웃되었습니다.");
    }

    /**
     * 회원탈퇴
     * DELETE /users/me
     */
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteAccount(@AuthenticationPrincipal User user) {

        if (user == null) {
            log.warn("User is null in DELETE /user/me");
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }

        log.info("Account deletion requested - userId: {}, email: {}", user.getUserId(), user.getEmail());

        authService.deleteAccount(user.getUserId());

        return ResponseEntity.ok("회원탈퇴가 완료되었습니다.");
    }

}
