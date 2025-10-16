package com.mysite.knitly.domain.product.review.dto;

import com.mysite.knitly.domain.product.review.entity.Review;
import com.mysite.knitly.domain.product.review.service.ReviewService;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewListResponse(
        Long reviewId,
        Byte rating,
        String content,
        LocalDateTime createdAt,
        String userName,
        List<String> reviewImageUrls
) {
    public static ReviewListResponse from(Review review, List<String> imageUrls) {
        return new ReviewListResponse(
                review.getReviewId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUser().getName(),
                imageUrls
        );
    }
}
