package com.mysite.knitly.domain.product.review.controller;

import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewCreateResponse;
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


@RestController
@RequiredArgsConstructor
@RequestMapping
public class ReviewController {
    private final ReviewService reviewService;

    // 1. 리뷰 작성 폼용 상품 정보 조회
    @GetMapping("products/{productId}/review")
    public ResponseEntity<ReviewCreateResponse> getReviewInfo(@PathVariable Long productId) {
        ReviewCreateResponse response = reviewService.getReviewFormInfo(productId);
        return ResponseEntity.ok(response);
    }

    // 1. 리뷰 등록
    @PostMapping("products/{productId}/reviews")
    public ResponseEntity<Void> createReview(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId,
            @Valid @ModelAttribute ReviewCreateRequest request
    ) {
        reviewService.createReview(productId, user, request);
        return ResponseEntity.ok().build();
    }

    // 2️. 리뷰 소프트 삭제(마이 페이지에서)
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal User user,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(reviewId, user);
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
