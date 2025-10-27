package com.mysite.knitly.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentCancelResponse(
        Long paymentId,
        String paymentKey,
        String orderId,
        PaymentStatus status,
        Long cancelAmount,
        String cancelReason,
        LocalDateTime canceledAt
) {
}