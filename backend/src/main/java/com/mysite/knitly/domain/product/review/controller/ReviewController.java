package com.mysite.knitly.domain.product.review.controller;

import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewDeleteRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewListResponse;
import com.mysite.knitly.domain.product.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ReviewController {
    private final ReviewService reviewService;

    // 1️. 리뷰 등록
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ReviewListResponse> createReview(
            @PathVariable Long productId,
            @RequestParam Long userId,
            @RequestParam("content") String content,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "images", required = false) List<MultipartFile> images
    ) {
        ReviewCreateRequest request = new ReviewCreateRequest(rating, content, images);
        ReviewListResponse response = reviewService.createReview(productId, userId, request);
        return ResponseEntity.ok(response);
    }

    // 2️. 리뷰 소프트 삭제
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestParam Long userId
    ) {
        ReviewDeleteRequest request = new ReviewDeleteRequest(userId);
        reviewService.deleteReview(reviewId, request);
        return ResponseEntity.noContent().build();
    }

    // 3. 특정 상품 리뷰 목록 조회
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<ReviewListResponse>> getReviewsByProduct(
            @PathVariable Long productId
    ) {
        List<ReviewListResponse> reviews = reviewService.getReviewsByProduct(productId);
        return ResponseEntity.ok(reviews);
    }
}
