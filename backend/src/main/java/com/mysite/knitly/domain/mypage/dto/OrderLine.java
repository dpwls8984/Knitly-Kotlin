package com.mysite.knitly.domain.mypage.dto;

public record OrderLine(
        Long orderItemId,
        Long productId,
        String productTitle,
        int quantity,
        Double orderPrice,
        boolean isReviewed
) {}
