package com.mysite.knitly.domain.home.dto;

import java.time.LocalDate;

public record LatestReviewItem(
        Long reviewId,
        Long productId,
        String productTitle,
        String productThumbnailUrl,
        Integer rating,
        String content,
        LocalDate createdDate
) {}
