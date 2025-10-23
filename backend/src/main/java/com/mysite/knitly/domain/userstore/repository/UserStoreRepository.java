package com.mysite.knitly.domain.userstore.repository;

import com.mysite.knitly.domain.userstore.entity.UserStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStoreRepository extends JpaRepository<UserStore, Long> {

    /**
     * userId로 UserStore 조회
     */
    Optional<UserStore> findByUser_UserId(Long userId);
}
