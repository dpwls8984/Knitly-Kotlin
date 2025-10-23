package com.mysite.knitly.domain.home.dto;

import com.mysite.knitly.domain.product.product.dto.ProductListResponse;
import java.util.List;

public record HomeSummaryResponse(
        List<ProductListResponse> popularProducts, // 인기 상품 Top5
        List<LatestReviewItem> latestReviews,      // 최신 리뷰
        List<LatestPostItem> latestPosts           // 최신 커뮤니티 글
) {}
