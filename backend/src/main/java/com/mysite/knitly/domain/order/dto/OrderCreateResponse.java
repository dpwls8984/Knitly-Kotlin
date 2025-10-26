package com.mysite.knitly.domain.order.dto;

import com.mysite.knitly.domain.order.entity.Order;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record OrderCreateResponse(
        Long orderId,
        String tossOrderId,  // 토스페이먼츠용 주문번호 추가
        LocalDateTime orderDate,
        Double totalPrice,
        List<OrderItemInfo> orderItems
) {
    public static OrderCreateResponse from(Order order) {
        List<OrderItemInfo> itemInfos = order.getOrderItems().stream()
                .map(OrderItemInfo::from)
                .collect(Collectors.toList());

        return new OrderCreateResponse(
                order.getOrderId(),
                order.getTossOrderId(),
                order.getCreatedAt(),
                order.getTotalPrice(),
                itemInfos
        );
    }

    // 주문에 포함된 개별 상품 정보를 담을 내부 레코드
    public record OrderItemInfo(
            Long productId,
            String title,
            Double orderPrice
    ) {
        public static OrderItemInfo from(com.mysite.knitly.domain.order.entity.OrderItem orderItem) {
            return new OrderItemInfo(
                    orderItem.getProduct().getProductId(),
                    orderItem.getProduct().getTitle(),
                    orderItem.getOrderPrice()
            );
        }
    }
}