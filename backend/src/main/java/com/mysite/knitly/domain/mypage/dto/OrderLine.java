package com.mysite.knitly.domain.mypage.dto;

public record OrderLine(
        Long productId,
        String productTitle,
        int quantity,
        Double orderPrice
) {}
