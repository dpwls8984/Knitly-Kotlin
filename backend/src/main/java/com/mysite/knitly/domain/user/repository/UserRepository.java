package com.mysite.knitly.domain.user.repository;

import com.mysite.knitly.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long userId);
    Optional<User> findBySocialId(String socialId);
}
