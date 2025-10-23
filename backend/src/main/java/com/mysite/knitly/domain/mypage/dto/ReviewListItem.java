package com.mysite.knitly.domain.mypage.dto;

import java.time.LocalDate;

public record ReviewListItem(
        Long reviewId,
        Long productId,
        String productTitle,
        String productThumbnailUrl,
        Integer rating,
        String content,             // 프론트에서 접기/펼치기
        LocalDate createdDate
) {}
