package com.mysite.knitly.domain.order.entity;

import com.mysite.knitly.domain.product.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Double orderPrice; // 주문 당시의 상품 가격

    @Column(nullable = false)
    private int quantity; // 수량 (항상 1)

    @Builder
    public OrderItem(Order order, Product product, Double orderPrice, int quantity) {
        this.order = order;
        this.product = product;
        this.orderPrice = orderPrice;
        this.quantity = quantity;
    }

    //== 연관관계 편의 메서드용 Setter ==//
    public void setOrder(Order order) {
        this.order = order;
    }
}

/*
CREATE TABLE `order_items` (
        `order_item_id` BIGINT          NOT NULL    AUTO_INCREMENT,
    `order_id`      BIGINT          NOT NULL,
        `product_id`    BIGINT          NOT NULL,
        `order_price`   DECIMAL(10, 2)  NOT NULL,  -- 주문 당시의 개별 상품 가격
    `quantity`      INT             NOT NULL,  -- 주문 수량 (항상 1)
PRIMARY KEY (`order_item_id`),
FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`)
);
*/
