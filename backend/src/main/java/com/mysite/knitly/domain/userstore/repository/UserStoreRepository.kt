package com.mysite.knitly.domain.userstore.repository

import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.domain.userstore.entity.UserStore
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserStoreRepository : JpaRepository<UserStore, Long> {

    /**
     * userId로 UserStore 조회
     */
    fun findByUser_UserId(userId: Long): Optional<UserStore>

    fun existsByUser(user: User): Boolean
}