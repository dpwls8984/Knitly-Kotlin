package com.mysite.knitly.domain.mypage.dto;

import java.math.BigDecimal;

public record OrderLine(
        Long productId,
        String productTitle,
        int quantity,
        BigDecimal orderPrice
) {}
