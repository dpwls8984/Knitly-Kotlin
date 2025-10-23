package com.mysite.knitly.domain.mypage.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record OrderCardResponse(
        Long orderId,
        LocalDateTime orderedAt,
        Double totalPrice,
        List<OrderLine> items
) {
    public static OrderCardResponse of(Long orderId, LocalDateTime orderedAt, Double totalPrice) {
        return new OrderCardResponse(orderId, orderedAt, totalPrice, new ArrayList<>());
    }
}
