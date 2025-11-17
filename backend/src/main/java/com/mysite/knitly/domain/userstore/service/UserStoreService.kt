package com.mysite.knitly.domain.userstore.service

import com.mysite.knitly.domain.userstore.repository.UserStoreRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserStoreService(
    private val userStoreRepository: UserStoreRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 스토어 설명 조회
     *
     * @param userId 사용자 ID
     * @return 스토어 설명
     */
    fun getStoreDetail(userId: Long): String {
        val userStore = userStoreRepository.findByUser_UserId(userId)
            .orElseThrow { IllegalArgumentException("스토어를 찾을 수 없습니다.") }

        return userStore.storeDetail ?: "안녕하세요! 제 스토어에 오신 것을 환영합니다."
    }

    /**
     * 스토어 설명 업데이트
     *
     * @param userId 사용자 ID
     * @param storeDetail 스토어 설명
     */
    @Transactional
    fun updateStoreDetail(userId: Long, storeDetail: String) {
        log.info("Updating store detail for userId: {}", userId)

        val userStore = userStoreRepository.findByUser_UserId(userId)
            .orElseThrow { IllegalArgumentException("스토어를 찾을 수 없습니다.") }

        userStore.updateStoreDetail(storeDetail)

        log.info("Store detail updated successfully for userId: {}", userId)
    }
}