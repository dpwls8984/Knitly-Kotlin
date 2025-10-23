package com.mysite.knitly.domain.order.dto;

public record EmailNotificationDto(
        Long orderId,
        Long userId,
        String userEmail
) {
}
