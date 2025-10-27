package com.mysite.knitly.domain.product.review.repository;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.review.entity.Review;
import jakarta.persistence.Entity;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @EntityGraph(attributePaths = {"user", "reviewImages"})
    List<Review> findByProduct_ProductIdAndIsDeletedFalse(Long productId);
    // 추후 페이징용

    @EntityGraph(attributePaths = {"user", "reviewImages"})
    Page<Review> findByProduct_ProductIdAndIsDeletedFalse(Long productId, Pageable pageable);

    //마이페이지 리뷰 조회
    @EntityGraph(attributePaths = {"product", "reviewImages"})
    List<Review> findByUser_UserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    long countByUser_UserIdAndIsDeletedFalse(Long userId);

    long countByProductAndIsDeletedFalse(Product product);

}