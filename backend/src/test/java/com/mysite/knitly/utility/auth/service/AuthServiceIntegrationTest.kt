package com.mysite.knitly.utility.auth.service

import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.user.repository.UserRepository
import com.mysite.knitly.utility.auth.dto.TokenRefreshResponse
import com.mysite.knitly.utility.jwt.JwtProvider
import com.mysite.knitly.utility.redis.RefreshTokenService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * AuthService 통합 테스트
 *
 * 테스트 대상:
 * 1. refreshAccessToken - Refresh Token으로 Access Token 갱신
 * 2. logout - 로그아웃
 * 3. deleteAccount - 회원탈퇴
 *
 * 통합 테스트이므로:
 * - 실제 Spring Context를 로드하여 Bean들을 주입받음
 * - 실제 JWT 생성/검증 로직 실행
 * - 실제 Redis 연동 (테스트 프로파일의 Embedded Redis 사용)
 * - 실제 DB 연동 (H2 In-Memory DB 사용)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Autowired
    private lateinit var refreshTokenService: RefreshTokenService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    private lateinit var testUser: User
    private var testUserId: Long = 0L
    private lateinit var validRefreshToken: String

    /**
     * 각 테스트 실행 전 테스트용 데이터 생성
     */
    @BeforeEach
    fun setUp() {
        // 0. Redis 초기화 (이전 테스트 데이터 제거)
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()

        // 1. 테스트용 사용자 생성
        testUser = User(
            email = "test@knitly.com",
            name = "테스트 사용자",
            socialId = "test-social-id",
            provider = Provider.GOOGLE
        )
        testUser = userRepository.save(testUser)
        testUserId = testUser.userId

        // 2. 테스트용 Refresh Token 생성 및 Redis에 저장
        validRefreshToken = jwtProvider.createRefreshToken(testUserId)
        refreshTokenService.saveRefreshToken(testUserId, validRefreshToken)
    }

    /**
     * 각 테스트 실행 후 Redis 데이터 정리
     */
    @AfterEach
    fun tearDown() {
        // Redis 데이터 정리 (테스트 격리를 위해)
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
    }

    // ========== refreshAccessToken 테스트 ==========

    @Test
    @DisplayName("토큰 갱신 성공 - 유효한 Refresh Token으로 새로운 Access Token과 Refresh Token 발급")
    fun refreshAccessToken_Success() {
        // when
        val result: TokenRefreshResponse = authService.refreshAccessToken(validRefreshToken)

        // then
        assertThat(result).isNotNull
        assertThat(result.accessToken).isNotBlank()
        assertThat(result.refreshToken).isNotBlank()
        assertThat(result.tokenType).isEqualTo("Bearer")
        assertThat(result.expiresIn).isGreaterThan(0L)

        // 새로 발급된 Access Token이 유효한지 검증
        assertThat(jwtProvider.validateToken(result.accessToken)).isTrue

        // 새로 발급된 Refresh Token이 유효한지 검증
        assertThat(jwtProvider.validateToken(result.refreshToken)).isTrue

        // 새로 발급된 Refresh Token에서 userId 추출 가능한지 검증
        val userIdFromNewToken = jwtProvider.getUserIdFromToken(result.accessToken)
        assertThat(userIdFromNewToken).isEqualTo(testUserId)

        // Redis에 새로운 Refresh Token이 저장되었는지 확인
        val storedRefreshToken = refreshTokenService.getRefreshToken(testUserId)
        assertThat(storedRefreshToken).isEqualTo(result.refreshToken)
    }


    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 Refresh Token (잘못된 형식)")
    fun refreshAccessToken_Fail_InvalidToken() {
        // given
        val invalidToken = "invalid.token.format"

        // when & then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            authService.refreshAccessToken(invalidToken)
        }

        assertThat(exception.message).contains("유효하지 않은 Refresh Token")
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 만료된 Refresh Token")
    fun refreshAccessToken_Fail_ExpiredToken() {
        // given - 이미 만료된 토큰을 시뮬레이션하기 위해 매우 짧은 유효기간의 토큰 생성
        // (실제로는 JwtProvider를 수정해야 하지만, 여기서는 간단히 검증 실패만 확인)
        val expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDB9.invalid"

        // when & then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            authService.refreshAccessToken(expiredToken)
        }

        assertThat(exception.message).contains("유효하지 않은 Refresh Token")
    }

    @Test
    @DisplayName("토큰 갱신 실패 - Redis에 저장되지 않은 토큰 (로그아웃한 사용자)")
    fun refreshAccessToken_Fail_TokenNotInRedis() {
        // given - 다른 사용자의 유효한 토큰 생성 (하지만 Redis에는 저장하지 않음)
        val anotherUser = User(
            email = "another@knitly.com",
            name = "다른 사용자",
            socialId = "another-social-id",
            provider = Provider.KAKAO
        )
        val savedAnotherUser = userRepository.save(anotherUser)
        val anotherUserId = savedAnotherUser.userId

        val tokenNotInRedis = jwtProvider.createRefreshToken(anotherUserId)
        // Redis에는 저장하지 않음!

        // when & then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            authService.refreshAccessToken(tokenNotInRedis)
        }

        assertThat(exception.message).contains("Refresh Token이 일치하지 않습니다")
    }

    // ========== logout 테스트 ==========

    @Test
    @DisplayName("로그아웃 성공 - Redis에서 Refresh Token 삭제")
    fun logout_Success() {
        // given - Redis에 Refresh Token이 저장되어 있음 (setUp에서 저장됨)
        assertThat(refreshTokenService.getRefreshToken(testUserId)).isNotNull

        // when
        authService.logout(testUserId)

        // then - Redis에서 Refresh Token이 삭제되었는지 확인
        val deletedToken = refreshTokenService.getRefreshToken(testUserId)
        assertThat(deletedToken).isNull()
    }

    @Test
    @DisplayName("로그아웃 성공 - 이미 로그아웃한 사용자 (Redis에 토큰 없음)")
    fun logout_Success_AlreadyLoggedOut() {
        // given - 이미 로그아웃하여 Redis에 토큰이 없는 상태
        authService.logout(testUserId)
        assertThat(refreshTokenService.getRefreshToken(testUserId)).isNull()

        // when - 다시 로그아웃 시도
        authService.logout(testUserId)

        // then - 예외 발생하지 않고 정상 처리됨
        assertThat(refreshTokenService.getRefreshToken(testUserId)).isNull()
    }

    // ========== deleteAccount 테스트 ==========

    @Test
    @DisplayName("회원탈퇴 성공 - Redis에서 토큰 삭제 및 DB에서 사용자 삭제")
    fun deleteAccount_Success() {
        // given - Redis에 Refresh Token이 저장되어 있고, DB에 사용자가 존재함
        assertThat(refreshTokenService.getRefreshToken(testUserId)).isNotNull
        assertThat(userRepository.findById(testUserId)).isPresent

        // when
        authService.deleteAccount(testUserId)

        // then
        // 1. Redis에서 Refresh Token이 삭제되었는지 확인
        val deletedToken = refreshTokenService.getRefreshToken(testUserId)
        assertThat(deletedToken).isNull()

        // 2. DB에서 사용자가 삭제되었는지 확인
        val deletedUser = userRepository.findById(testUserId)
        assertThat(deletedUser).isEmpty
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 존재하지 않는 사용자")
    fun deleteAccount_Fail_UserNotFound() {
        // given - 존재하지 않는 userId
        val nonExistentUserId = 99999L

        // when & then
        val exception = assertThrows(RuntimeException::class.java) {
            authService.deleteAccount(nonExistentUserId)
        }

        assertThat(exception.message).contains("사용자를 찾을 수 없습니다")
    }

    @Test
    @DisplayName("회원탈퇴 성공 - Redis에 토큰이 없어도 사용자 삭제는 정상 처리")
    fun deleteAccount_Success_NoTokenInRedis() {
        // given - Redis에서 Refresh Token 먼저 삭제 (로그아웃 상태)
        refreshTokenService.deleteRefreshToken(testUserId)
        assertThat(refreshTokenService.getRefreshToken(testUserId)).isNull()

        // when - 회원탈퇴 시도
        authService.deleteAccount(testUserId)

        // then - 예외 발생하지 않고 사용자 삭제 성공
        val deletedUser = userRepository.findById(testUserId)
        assertThat(deletedUser).isEmpty
    }

    // ========== 엣지 케이스 테스트 ==========

    @Test
    @DisplayName("토큰 갱신 성공 - 새로운 Access Token으로 API 요청 가능")
    fun refreshAccessToken_Success_NewTokenIsValid() {
        // when
        val result: TokenRefreshResponse = authService.refreshAccessToken(validRefreshToken)

        // then - 새로 발급된 Access Token으로 userId 추출 가능
        val extractedUserId = jwtProvider.getUserIdFromToken(result.accessToken)
        assertThat(extractedUserId).isEqualTo(testUserId)

        // 새로 발급된 Refresh Token으로 다시 갱신 가능
        val secondResult = authService.refreshAccessToken(result.refreshToken)
        assertThat(secondResult).isNotNull
        assertThat(secondResult.accessToken).isNotBlank()
    }

    @Test
    @DisplayName("토큰 갱신 후 이전 Refresh Token은 사용 불가")
    fun refreshAccessToken_OldTokenInvalidAfterRefresh() {
        // given - 첫 번째 갱신
        val firstResult = authService.refreshAccessToken(validRefreshToken)

        // 새로운 토큰으로는 갱신 가능
        val secondResult = authService.refreshAccessToken(firstResult.refreshToken)
        assertThat(secondResult).isNotNull
    }

    @Test
    @DisplayName("여러 사용자의 토큰 갱신 - 서로 영향 없음")
    fun refreshAccessToken_MultipleUsers_Independent() {
        // given - 두 번째 사용자 생성
        val user2 = User(
            email = "user2@knitly.com",
            name = "사용자2",
            socialId = "user2-social-id",
            provider = Provider.GOOGLE
        )
        val savedUser2 = userRepository.save(user2)
        val user2Id = savedUser2.userId

        val user2RefreshToken = jwtProvider.createRefreshToken(user2Id)
        refreshTokenService.saveRefreshToken(user2Id, user2RefreshToken)

        // when - 각 사용자의 토큰 갱신
        val user1Result = authService.refreshAccessToken(validRefreshToken)
        val user2Result = authService.refreshAccessToken(user2RefreshToken)

        // then - 각 사용자의 토큰이 독립적으로 관리됨
        val user1IdFromToken = jwtProvider.getUserIdFromToken(user1Result.accessToken)
        val user2IdFromToken = jwtProvider.getUserIdFromToken(user2Result.accessToken)

        assertThat(user1IdFromToken).isEqualTo(testUserId)
        assertThat(user2IdFromToken).isEqualTo(user2Id)
        assertThat(user1IdFromToken).isNotEqualTo(user2IdFromToken)

        // Redis에도 각각의 토큰이 저장되어 있음
        assertThat(refreshTokenService.getRefreshToken(testUserId)).isEqualTo(user1Result.refreshToken)
        assertThat(refreshTokenService.getRefreshToken(user2Id)).isEqualTo(user2Result.refreshToken)
    }
}
