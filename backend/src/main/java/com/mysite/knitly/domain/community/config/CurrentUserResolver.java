package com.mysite.knitly.domain.community.config;

import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * SecurityContext의 principal이 도메인 User 또는 UserDetails 어떤 것이든
 * 최종적으로 도메인 User 엔티티로 환원해 주는 어댑터.
 * (팀원의 JwtAuthenticationFilter, UserService 수정 없이 동작)
 */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserService userService;

    // 인증되어 있으면 User, 아니면 null
    public User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();

        // 이미 도메인 User인 경우
        if (principal instanceof User u) return u;

        // UserDetails(email) 형태인 경우
        if (principal instanceof UserDetails ud) {
            String email = ud.getUsername(); // 보통 username = email
            return findByEmailIfExists(email);
        }

        // 문자열 principal (예: anonymousUser)
        if (principal instanceof String s) {
            if ("anonymousUser".equalsIgnoreCase(s)) return null;
            return findByEmailIfExists(s);
        }

        return null;
    }

    // UserService에 findByEmail이 있을 때만 리플렉션으로 호출
    private User findByEmailIfExists(String email) {
        try {
            var method = UserService.class.getMethod("findByEmail", String.class);
            Object result = method.invoke(userService, email);
            if (result instanceof User) return (User) result;
        } catch (Exception ignore) {
            // 메서드가 없거나 호출 실패 시 무시
        }
        return null;
    }
}
