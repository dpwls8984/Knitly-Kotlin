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
    private User user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Double totalPrice;

    // Order가 저장될 때 OrderItem도 함께 저장되도록 Cascade 설정
    // ophanRemoval : OrderItem이 Order에서 제거되면 DB에서도 삭제
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    public Order(User user, Double totalPrice) {
        this.user = user;
        this.totalPrice = totalPrice;
    }

    //== 연관관계 편의 메서드 ==//
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this); // 양방향 관계 설정
    }
}

/*
CREATE TABLE `orders` (
    `order_id`      BIGINT          NOT NULL    AUTO_INCREMENT,
    `user_id`       BINARY(16)      NOT NULL,
    `created_at`    DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    `total_price`   DECIMAL(10, 2)  NOT NULL,  -- 주문 총액
    PRIMARY KEY (`order_id`)
);
*/