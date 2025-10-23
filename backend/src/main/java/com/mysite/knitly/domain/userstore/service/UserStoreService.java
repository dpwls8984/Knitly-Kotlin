package com.mysite.knitly.domain.userstore.service;

import com.mysite.knitly.domain.userstore.entity.UserStore;
import com.mysite.knitly.domain.userstore.repository.UserStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserStoreService {

    private final UserStoreRepository userStoreRepository;

    /**
     * 스토어 설명 조회
     *
     * @param userId 사용자 ID
     * @return 스토어 설명
     */
    public String getStoreDetail(Long userId) {
        UserStore userStore = userStoreRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("스토어를 찾을 수 없습니다."));

        return userStore.getStoreDetail() != null
                ? userStore.getStoreDetail()
                : "안녕하세요! 제 스토어에 오신 것을 환영합니다.";  // 기본값
    }

    /**
     * 스토어 설명 업데이트
     *
     * @param userId 사용자 ID
     * @param storeDetail 스토어 설명
     */
    @Transactional
    public void updateStoreDetail(Long userId, String storeDetail) {
        log.info("Updating store detail for userId: {}", userId);

        UserStore userStore = userStoreRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("스토어를 찾을 수 없습니다."));

        userStore.updateStoreDetail(storeDetail);

        log.info("Store detail updated successfully for userId: {}", userId);
    }
}
