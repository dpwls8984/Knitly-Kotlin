package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;

import java.util.List;

public record ProductRegisterResponse (
        Long productId,
        String title,
        String description,
        ProductCategory productCategory,
        String sizeInfo,
        Double price,
        String createdAt,
        //Integer purchaseCount,
        Integer stockQuantity,
        //Integer likeCount,
        Long designId,
        List<String> productImageUrls
) {
    public static ProductRegisterResponse from(Product product, List<String> imageUrls) {
        return new ProductRegisterResponse(
                product.getProductId(),
                product.getTitle(),
                product.getDescription(),
                product.getProductCategory(),
                product.getSizeInfo(),
                product.getPrice(),
                product.getCreatedAt().toString(),
                //product.getPurchaseCount(),
                product.getStockQuantity(),
                //product.getLikeCount(),
                product.getDesign().getDesignId(),
                imageUrls
        );
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
//        `user_id`	BINARY(16)	NOT NULL,
//	`purchase_count`	INT	NOT NULL	DEFAULT 0	COMMENT '누적수 분리?',
//        `is_deleted`	BOOLEAN	NOT NULL	DEFAULT FALSE	COMMENT '소프트 딜리트',
//        `stock_quantity`	INT	NULL	COMMENT 'null 이면 상시 판매 / 0~숫자 는 한정판매',
//        `like_count`	INT	NOT NULL	DEFAULT 0,
//        `design_id`	BIGINT	NOT NULL	DEFAULT AUTO_INCREMENT,
//        `avg_review_rating`	DECIMAL(3,2)	NULL
//);