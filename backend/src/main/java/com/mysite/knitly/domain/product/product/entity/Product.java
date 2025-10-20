package com.mysite.knitly.domain.product.product.entity;

import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "products")
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory productCategory; // 'TOP', 'BOTTOM', 'OUTER', 'BAG', 'ETC'

    @Column(nullable = false)
    private String sizeInfo;

    @Column(nullable = false, columnDefinition = "DECIMAL(10,2)")
    private Double price; // DECIMAL(10,2)

    @Column(nullable = false)
    @CreatedDate
    private LocalDateTime createdAt; // DATETIME

    @ManyToOne(fetch = FetchType.LAZY)
    //Cascade 안하는 이유 : User 삭제시 Product도 삭제되면 안됨
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer purchaseCount; // 누적수

    @Column(nullable = false)
    private Boolean isDeleted; // 소프트 딜리트

    @Column
    private Integer stockQuantity; // null 이면 상시 판매 / 0~숫자 는 한정판매

    @Column(nullable = false)
    private Integer likeCount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    //Cascade 안하는 이유 : Design 삭제시 Product도 삭제되면 안됨
    private Design design;

    @Column
    private Double avgReviewRating; // DECIMAL(3,2)

    //상품 수정하는 로직 추가
    public void update(String description, ProductCategory productCategory, String sizeInfo, Integer stockQuantity) {
        this.description = description;
        this.productCategory = productCategory;
        this.sizeInfo = sizeInfo;
        this.stockQuantity = stockQuantity;
    }

    //소프트 딜리트 로직 추가
    public void softDelete() {
        this.isDeleted = true;
    }

    //재고 수량 감소 메서드 추가
    public void decreaseStock(int quantity) {
        // 1. 상시 판매 상품(재고가 null)인 경우는 로직을 실행하지 않음
        if (this.stockQuantity == null) {
            return;
        }

        // 2. 남은 재고보다 많은 수량을 주문하면 예외 발생
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new ServiceException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT);
        }

        // 3. 재고 차감
        this.stockQuantity = restStock;
    }
}

//CREATE TABLE `products` (
//        `product_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `title`	VARCHAR(30)	NOT NULL,
//	`description`	TEXT	NOT NULL,
//        `product_category`	ENUM('TOP', 'BOTTOM', 'OUTER', 'BAG', 'ETC')	NOT NULL	COMMENT '상의, 하의, 아우터, 가방, 기타',
//        `size_info`	VARCHAR(255)	NOT NULL,
//	`price`	DECIMAL(10,2)	NOT NULL	COMMENT '무료 구분',
//        `created_at`	DATETIME	NOT NULL	DEFAULT CURRENT_TIMESTAMP,
//        `user_id`	BIGINT	NOT NULL,
//        `purchase_count`	INT	NOT NULL	DEFAULT 0	COMMENT '누적수 분리?',
//        `is_deleted`	BOOLEAN	NOT NULL	DEFAULT FALSE	COMMENT '소프트 딜리트',
//        `stock_quantity`	INT	NULL	COMMENT 'null 이면 상시 판매 / 0~숫자 는 한정판매',
//        `like_count`	INT	NOT NULL	DEFAULT 0,
//        `design_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `avg_review_rating`	DECIMAL(3,2)	NULL
//);