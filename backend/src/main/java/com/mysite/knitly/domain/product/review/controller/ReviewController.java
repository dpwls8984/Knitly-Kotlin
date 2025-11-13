package com.mysite.knitly.domain.product.review.controller;

import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewCreateResponse;
import com.mysite.knitly.domain.product.review.dto.ReviewDeleteRequest;
import com.mysite.knitly.domain.product.review.dto.ReviewListResponse;
import com.mysite.knitly.domain.product.review.service.ReviewService;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @GetMapping("/reviews/form")
    public ResponseEntity<ReviewCreateResponse> getReviewInfo(@RequestParam Long orderItemId) {
        ReviewCreateResponse response = reviewService.getReviewFormInfo(orderItemId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reviews")
    public ResponseEntity<Void> createReview(
            @AuthenticationPrincipal User user,
            @RequestParam Long orderItemId,
            @Valid @ModelAttribute ReviewCreateRequest request
    ) {
        reviewService.createReview(orderItemId, user, request);
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
    public ResponseEntity<Page<ReviewListResponse>> getReviewsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReviewListResponse> reviews = reviewService.getReviewsByProduct(productId, page, size);
        return ResponseEntity.ok(reviews);
    }
}
