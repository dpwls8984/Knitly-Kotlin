package com.mysite.knitly.domain.product.review.dto

import com.mysite.knitly.domain.product.review.entity.Review
import java.time.LocalDateTime

data class ReviewListResponse(
    val reviewId: Long,

    val rating: Int,

    val content: String,

    val createdAt: LocalDateTime,

    val userName: String,

    val reviewImageUrls: List<String> = emptyList()
) {
    companion object {
        fun from(review: Review, imageUrls: List<String>): ReviewListResponse {
            return ReviewListResponse(
                reviewId = review.reviewId,
                rating = review.rating,
                content = review.content,
                createdAt = review.createdAt,
                userName = review.user.name,
                reviewImageUrls = imageUrls
            )
        }
    }
}
