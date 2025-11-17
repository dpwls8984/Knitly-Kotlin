package com.mysite.knitly.domain.user.repository

import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long> {

    /**
     * socialId와 provider로 사용자 조회
     * 예: Google의 sub 값과 GOOGLE로 검색
     */
    fun findBySocialIdAndProvider(socialId: String, provider: Provider): Optional<User>

    /**
     * 이메일로 사용자 존재 여부 확인 --> 추후 이메일중복 가입 방지 기능으로 확장가능
     */
    fun existsByEmail(email: String): Boolean

    override fun findById(userId: Long): Optional<User>

    fun findBySocialId(socialId: String): Optional<User>
}