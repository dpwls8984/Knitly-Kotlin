package com.mysite.knitly.domain.product.review.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class ReviewCreateRequest(
    @field:NotNull(message = "리뷰 점수는 필수입니다.")
    @field:Min(value = 1, message = "평점은 1~5 사이여야 합니다.")
    @field:Max(value = 5, message = "평점은 1~5 사이여야 합니다.")
    val rating: Int?,

    @field:NotNull(message = "리뷰 내용은 필수입니다.")
    val content: String?,

    @field:Size(max = 10, message = "리뷰 이미지는 최대 10개까지 등록할 수 있습니다.")
    val reviewImageUrls: List<MultipartFile> = emptyList()
)