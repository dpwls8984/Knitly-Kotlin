package com.mysite.knitly.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentMethod;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentDetailResponse(
        Long paymentId,
        String paymentKey,
        String orderId,
        String orderName,
        String mid,
        PaymentMethod method,
        Long totalAmount,
        PaymentStatus status,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime canceledAt,
        String cancelReason,
        Long buyerId,
        String buyerName
) {

    // Payment 엔티티로부터 기본 정보 생성
    public static PaymentDetailResponse from(Payment payment) {
        return PaymentDetailResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentKey(payment.getTossPaymentKey())
                .orderId(payment.getTossOrderId())
                .mid(payment.getMid())
                .method(payment.getPaymentMethod())
                .totalAmount(payment.getTotalAmount())
                .status(payment.getPaymentStatus())
                .requestedAt(payment.getRequestedAt())
                .approvedAt(payment.getApprovedAt())
                .canceledAt(payment.getCanceledAt())
                .cancelReason(payment.getCancelReason())
                .buyerId(payment.getBuyer().getUserId())
                .buyerName(payment.getBuyer().getName())
                .build();
    }
}