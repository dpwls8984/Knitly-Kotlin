package com.mysite.knitly.domain.user.service

import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.domain.userstore.entity.UserStore
import com.mysite.knitly.domain.userstore.repository.UserStoreRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val userStoreRepository: UserStoreRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Google OAuth로 로그인한 사용자 처리
     * - 신규 사용자: 회원가입 처리
     * - 기존 사용자: 정보 조회
     */
    @Transactional
    fun processGoogleUser(socialId: String, email: String, name: String): User {
        // 1. 이미 가입된 사용자인지 확인
        return userRepository.findBySocialIdAndProvider(socialId, Provider.GOOGLE)
            .orElseGet {
                // 2. 신규 사용자면 회원가입
                log.info("신규 Google 사용자 가입: email={}, name={}", email, name)

                val newUser = User.createGoogleUser(socialId, email, name)
                val savedUser = userRepository.save(newUser)

                log.info("회원가입 완료: userId={}", savedUser.userId)
                savedUser
            }
    }

    @Transactional
    fun ensureUserStore(user: User) {
        if (!userStoreRepository.existsByUser(user)) {
            log.info("유저 스토어 생성: userId={}", user.userId)
            userStoreRepository.save(
                UserStore(
                    user = user,
                    storeDetail = "안녕하세요! 제 스토어에 오신 것을 환영합니다."
                )
            )
        } else {
            log.info("기존 스토어 존재: userId={}", user.userId)
        }
    }

    /**
     * userId로 사용자 조회
     */
    fun findById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다: $userId") }
    }

    /**
     * 회원탈퇴
     */
    @Transactional
    fun deleteUser(userId: Long) {
        val user = findById(userId)
        userRepository.delete(user)
        log.info("회원탈퇴 완료 - userId: {}, email: {}", userId, user.email)
    }
}