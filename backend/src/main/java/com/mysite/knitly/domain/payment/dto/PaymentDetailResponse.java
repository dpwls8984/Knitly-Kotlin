package com.mysite.knitly.domain.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentMethod;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentDetailResponse(
        String orderName,          // 주문 상품명
        String orderId,            // 주문번호 (tossOrderId)
        PaymentMethod method,      // 결제 수단
        Long totalAmount,          // 결제 금액
        PaymentStatus status,      // 결제 상태
        LocalDateTime requestedAt, // 결제 요청 시간
        LocalDateTime approvedAt,  // 결제 승인 시간
        LocalDateTime canceledAt,  // 취소 시간
        String cancelReason,       // 취소 사유
        String buyerName           // 구매자 이름
) {

    // Payment 엔티티로부터 기본 정보 생성
    public static PaymentDetailResponse from(Payment payment) {
        Order order = payment.getOrder();

        // 주문 상품명 생성 로직
        String orderName = generateOrderName(order);

        return PaymentDetailResponse.builder()
                .orderName(orderName)
                .orderId(payment.getTossOrderId())
                .method(payment.getPaymentMethod())
                .totalAmount(payment.getTotalAmount())
                .status(payment.getPaymentStatus())
                .requestedAt(payment.getRequestedAt())
                .approvedAt(payment.getApprovedAt())
                .canceledAt(payment.getCanceledAt())
                .cancelReason(payment.getCancelReason())
                .buyerName(payment.getBuyer().getName())
                .build();
    }

    private static String generateOrderName(Order order) {
        var orderItems = order.getOrderItems();

        if (orderItems == null || orderItems.isEmpty()) {
            return "상품 정보 없음";
        }

        String firstProductTitle = orderItems.get(0).getProduct().getTitle();
        int remainingCount = orderItems.size() - 1;

        if (remainingCount > 0) {
            return firstProductTitle + " 외 " + remainingCount + "개";
        } else {
            return firstProductTitle;
        }
    }
}