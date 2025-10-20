package com.mysite.knitly.domain.product.product.entity;

import com.mysite.knitly.domain.design.entity.Design;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "products")
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    private Long productId;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "ENUM('TOP', 'BOTTOM', 'OUTER', 'BAG', 'ETC')")
    private String productCategory; // 'TOP', 'BOTTOM', 'OUTER', 'BAG', 'ETC'

    @Column(nullable = false)
    private String sizeInfo;

    @Column(nullable = false, columnDefinition = "DECIMAL(10,2)")
    private Double price; // DECIMAL(10,2)

    @Column(nullable = false)
    @CreatedDate
    private String createdAt; // DATETIME

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