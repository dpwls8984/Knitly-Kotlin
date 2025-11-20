package com.mysite.knitly.domain.home.dto

import java.time.LocalDate

data class LatestReviewItem(
    val reviewId: Long,
    val productId: Long,
    val productTitle: String,
    val productThumbnailUrl: String?,
    val rating: Int,
    val content: String,
    val createdDate: LocalDate
)