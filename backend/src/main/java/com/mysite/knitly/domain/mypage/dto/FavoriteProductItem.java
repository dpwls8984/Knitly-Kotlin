package com.mysite.knitly.domain.mypage.dto;

import java.time.LocalDate;


// 마이페이지 - 내가 찜한 상품 조회

public record FavoriteProductItem(
        Long productId,
        String productTitle,
        String thumbnailUrl,
        Double price,
        Double averageRating,
        LocalDate likedAt
) {}
