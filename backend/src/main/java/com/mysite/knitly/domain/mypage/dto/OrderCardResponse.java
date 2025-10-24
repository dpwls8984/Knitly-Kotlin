package com.mysite.knitly.domain.mypage.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public record OrderCardResponse(
        Long orderId,
        LocalDateTime orderedAt,
        BigDecimal totalPrice,
        List<OrderLine> items
) {
    public static OrderCardResponse of(Long orderId, LocalDateTime orderedAt, BigDecimal totalPrice) {
        return new OrderCardResponse(orderId, orderedAt, totalPrice, new ArrayList<>());
    }
}
