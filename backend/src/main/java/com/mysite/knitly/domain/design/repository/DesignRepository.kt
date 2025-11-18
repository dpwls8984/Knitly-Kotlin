package com.mysite.knitly.domain.design.repository

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DesignRepository : JpaRepository<Design, Long> {
    fun findByUser(user: User): List<Design>
}
