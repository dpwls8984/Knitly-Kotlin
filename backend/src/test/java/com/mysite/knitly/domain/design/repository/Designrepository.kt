package com.mysite.knitly.domain.design.repository

import com.mysite.knitly.domain.design.entity.Design
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Design Repository
 *
 * 테스트에서 Design 엔티티를 저장하고 조회하기 위한 Repository
 */
@Repository
interface DesignRepository : JpaRepository<Design, Long> {
    // 기본 CRUD 메서드는 JpaRepository에서 제공
}