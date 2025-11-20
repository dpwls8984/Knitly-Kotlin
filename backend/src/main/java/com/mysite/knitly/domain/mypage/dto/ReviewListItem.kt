package com.mysite.knitly.domain.mypage.dto

import java.time.LocalDate

data class ReviewListItem(
    val reviewId: Long,
    val productId: Long,
    val productTitle: String,
    val productThumbnailUrl: String?,
    val rating: Int?,
    val content: String,
    val reviewImageUrls: List<String>,
    val createdDate: LocalDate
)
