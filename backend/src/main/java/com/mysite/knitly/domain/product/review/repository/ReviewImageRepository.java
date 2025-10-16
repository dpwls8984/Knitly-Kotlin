package com.mysite.knitly.domain.product.review.repository;

import com.mysite.knitly.domain.product.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    List<ReviewImage> findByReview_ReviewId(Long reviewId);
}
