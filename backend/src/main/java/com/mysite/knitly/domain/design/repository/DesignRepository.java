package com.mysite.knitly.domain.design.repository;

import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignRepository extends JpaRepository<Design,Long> {
    List<Design> findByUser(User user);
    Optional<Design> findByDesignId(Long designId);
}
