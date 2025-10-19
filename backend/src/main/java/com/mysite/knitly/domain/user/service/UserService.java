package com.mysite.knitly.domain.user.service;

import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.entity.enums.Provider;
import com.mysite.knitly.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Google OAuth로 로그인한 사용자 처리
     * - 신규 사용자: 회원가입 처리
     * - 기존 사용자: 정보 조회
     */
    @Transactional
    public User processGoogleUser(String socialId, String email, String name) {
        // 1. 이미 가입된 사용자인지 확인
        return userRepository.findBySocialIdAndProvider(socialId, Provider.GOOGLE)
                .orElseGet(() -> {
                    // 2. 신규 사용자면 회원가입
                    log.info("신규 Google 사용자 가입: email={}, name={}", email, name);

                    User newUser = User.createGoogleUser(socialId, email, name);
                    User savedUser = userRepository.save(newUser);

                    log.info("회원가입 완료: userId={}", savedUser.getUserId());
                    return savedUser;
                });
    }

    /**
     * userId로 사용자 조회
     */
    public User findById(java.util.UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }
}
