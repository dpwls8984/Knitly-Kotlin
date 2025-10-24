package com.mysite.knitly.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentMethod;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentConfirmResponse(
        Long paymentId,
        String paymentKey,
        String orderId,
        String orderName,
        PaymentMethod method,
        Long totalAmount,
        PaymentStatus status,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        String mid,
        CardInfo card,
        VirtualAccountInfo virtualAccount,
        EasyPayInfo easyPay
) {

    @Builder
    public record CardInfo(
            String company,
            String number,
            String installmentPlanMonths,
            String approveNo,
            String ownerType
    ) {}

    @Builder
    public record VirtualAccountInfo(
            String accountNumber,
            String bankCode,
            String customerName,
            LocalDateTime dueDate
    ) {}

    @Builder
    public record EasyPayInfo(
            String provider,
            Long amount
    ) {}

    /**
     * Payment 엔티티로부터 Response 생성
     */
    public static PaymentConfirmResponse from(Payment payment) {
        return PaymentConfirmResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentKey(payment.getTossPaymentKey())
                .orderId(payment.getTossOrderId())
                .method(payment.getPaymentMethod())
                .totalAmount(payment.getTotalAmount())
                .status(payment.getPaymentStatus())
                .requestedAt(payment.getRequestedAt())
                .approvedAt(payment.getApprovedAt())
                .mid(payment.getMid())
                .build();
    }
}