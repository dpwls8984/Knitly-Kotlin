package com.mysite.knitly.domain.user.repository;

import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.entity.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * socialId와 provider로 사용자 조회
     * 예: Google의 sub 값과 GOOGLE로 검색
     */
    Optional<User> findBySocialIdAndProvider(String socialId, Provider provider);

    /**
     * 이메일로 사용자 존재 여부 확인
     */
    boolean existsByEmail(String email);
}
