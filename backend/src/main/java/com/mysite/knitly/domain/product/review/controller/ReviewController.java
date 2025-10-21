package com.mysite.knitly.domain.product.review.controller;

import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewDeleteRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewListResponse;
import com.mysite.knitly.domain.product.review.service.ReviewService;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal User user,
            @PathVariable Long productId,
            @Valid @ModelAttribute ReviewCreateRequest request
    ) {
        Long currentUserId = user.getUserId();
        ReviewListResponse response = reviewService.createReview(productId, currentUserId, request);
        return ResponseEntity.ok(response);
    }

    // 2️. 리뷰 소프트 삭제(마이 페이지에서)
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal User user,
            @PathVariable Long reviewId
    ) {
        Long currentUserId = user.getUserId();

        ReviewDeleteRequest request = new ReviewDeleteRequest(currentUserId);
        reviewService.deleteReview(reviewId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // 3. 특정 상품 리뷰 목록 조회
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<ReviewListResponse>> getReviewsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<ReviewListResponse> reviews = reviewService.getReviewsByProduct(productId, page, size);
        return ResponseEntity.ok(reviews);
    }
}
