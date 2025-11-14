package com.mysite.knitly.domain.product.review.controller

import com.mysite.knitly.domain.product.review.dto.ReviewCreateRequest
import com.mysite.knitly.domain.product.review.dto.ReviewCreateResponse
import com.mysite.knitly.domain.product.review.dto.ReviewListResponse
import com.mysite.knitly.domain.product.review.service.ReviewService
import com.mysite.knitly.domain.user.entity.User
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping
class ReviewController(
    private val reviewService: ReviewService
) {

    @GetMapping("/reviews/form")
    fun getReviewInfo(@RequestParam orderItemId: Long): ResponseEntity<ReviewCreateResponse> {
        log.info { "[GET /reviews/form] 리뷰 작성 폼 조회 - orderItemId=$orderItemId" }

        val response = reviewService.getReviewFormInfo(orderItemId)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/reviews")
    fun createReview(
        @AuthenticationPrincipal user: User,
        @RequestParam orderItemId: Long,
        @ModelAttribute @Valid request: ReviewCreateRequest
    ): ResponseEntity<Unit> {
        log.info { "[POST /reviews] 리뷰 생성 - orderItemId=$orderItemId, userId=${user.userId}" }

        reviewService.createReview(orderItemId, user, request)

        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/reviews/{reviewId}")
    fun deleteReview(
        @AuthenticationPrincipal user: User,
        @PathVariable reviewId: Long
    ): ResponseEntity<Unit> {
        log.info { "[DELETE /reviews/$reviewId] 리뷰 삭제 - userId=${user.userId}" }

        reviewService.deleteReview(reviewId, user)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/products/{productId}/reviews")
    fun getReviewsByProduct(
        @PathVariable productId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<ReviewListResponse>> {
        log.info { "[GET /products/$productId/reviews] 상품 리뷰 목록 조회 - page=$page, size=$size" }

        val reviews = reviewService.getReviewsByProduct(productId, page, size)
        return ResponseEntity.ok(reviews)
    }
}