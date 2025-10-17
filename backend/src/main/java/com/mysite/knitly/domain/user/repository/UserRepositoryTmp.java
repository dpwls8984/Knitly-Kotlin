package com.mysite.knitly.domain.user.repository;

import com.mysite.knitly.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepositoryTmp extends JpaRepository<User, UUID> {
}
