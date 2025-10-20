package com.mysite.knitly.domain.product.product.repository;

import com.mysite.knitly.domain.product.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    //n+1 문제 해결을 위한 fetch join
    @Query("SELECT p FROM Product p JOIN FETCH p.user WHERE p.productId = :productId")
    Optional<Product> findByIdWithUser(Long productId);
}
