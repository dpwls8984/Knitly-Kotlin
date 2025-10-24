package com.mysite.knitly.domain.mypage.dto;

import java.time.LocalDate;
import java.util.List;

public record ReviewListItem(
        Long reviewId,
        Long productId,
        String productTitle,
        String productThumbnailUrl,
        Integer rating,
        String content,
        List<String> reviewImageUrls,// 프론트에서 접기/펼치기
        LocalDate createdDate
) {}
