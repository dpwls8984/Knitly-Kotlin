package com.mysite.knitly.domain.home.dto

import com.mysite.knitly.domain.product.product.dto.ProductListResponse

data class HomeSummaryResponse(
    val popularProducts: List<ProductListResponse>, // 인기 상품 Top5
    val latestReviews: List<LatestReviewItem>,      // 최신 리뷰
    val latestPosts: List<LatestPostItem>           // 최신 커뮤니티 글
)
