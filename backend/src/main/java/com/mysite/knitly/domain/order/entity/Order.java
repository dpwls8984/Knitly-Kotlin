package com.mysite.knitly.domain.order.entity;

import com.mysite.knitly.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "orders")
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 구매자

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Double totalPrice;

    // 토스페이먼츠 orderId (영문 대소문자, 숫자, -, _ 만 허용, 6자 이상 64자 이하)
    @Column(nullable = false, unique = true, length = 64)
    private String tossOrderId;

    // Order가 저장될 때 OrderItem도 함께 저장되도록 Cascade 설정
    // ophanRemoval : OrderItem이 Order에서 제거되면 DB에서도 삭제
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    public Order(User user, Double totalPrice, String tossOrderId) {
        this.user = user;
        this.totalPrice = totalPrice;
        this.tossOrderId = tossOrderId;
    }

    //== 생성 메서드 ==//
    public static Order create(User user, List<OrderItem> orderItems) {
        Order order = new Order();
        order.user = user; // 사용자 정보 설정

        // 토스페이먼츠 orderId 생성 (UUID 기반)
        order.tossOrderId = generateTossOrderId();

        // 모든 주문 상품을 추가하고 총액 계산
        double totalPrice = 0.0;
        for (OrderItem orderItem : orderItems) {
            order.addOrderItem(orderItem);
            totalPrice += orderItem.getOrderPrice();
        }
        order.totalPrice = totalPrice;

        return order;
    }

    //== 연관관계 편의 메서드 ==//
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this); // 양방향 관계 설정
    }

    /**
     * 토스페이먼츠 orderId 생성
     * 규칙: 영문 대소문자, 숫자, 특수문자 -, _ 로 이루어진 6자 이상 64자 이하
     * UUID 기반으로 생성하여 고유성 보장
     */
    private static String generateTossOrderId() {
        // UUID 생성 (하이픈 포함 36자, 토스페이먼츠 orderId 규칙에 부합)
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }
}

/*
CREATE TABLE `orders` (
    `order_id`      BIGINT          NOT NULL    AUTO_INCREMENT,
    `user_id`       BINARY(16)      NOT NULL,
    `created_at`    DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    `total_price`   DECIMAL(10, 2)  NOT NULL,  -- 주문 총액
    `toss_order_id` VARCHAR(64)     NOT NULL    UNIQUE,  -- 토스페이먼츠 주문번호
    PRIMARY KEY (`order_id`)
    INDEX idx_toss_order_id (`toss_order_id`)

);
*/