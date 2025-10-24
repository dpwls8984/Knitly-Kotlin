package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;

import java.time.LocalDateTime;

public record ProductListResponse(
        Long productId,
        String title,
        ProductCategory productCategory,
        Double price,
        Integer purchaseCount,
        Integer likeCount,

        boolean isLikedByUser,

        Integer stockQuantity,
        Double avgReviewRating,
        LocalDateTime createdAt,
        String thumbnailUrl, // 대표 이미지 URL (sortOrder = 1)
        Boolean isFree,     // 무료 여부
        Boolean isLimited,  // 한정판매 여부
        Boolean isSoldOut   // 품절 여부 (stockQuantity = 0)
) {
    // from 메서드는 그대로 유지하거나, 정적 팩토리 메서드로 변경할 수 있습니다.
    public static ProductListResponse from(Product product, boolean isLikedByUser) {
        // record는 생성자를 통해 필드를 초기화합니다.
        return new ProductListResponse(
                product.getProductId(),
                product.getTitle(),
                product.getProductCategory(),
                product.getPrice(),
                product.getPurchaseCount(),
                product.getLikeCount(),
                isLikedByUser,
                product.getStockQuantity(),
                product.getAvgReviewRating(),
                product.getCreatedAt(),
                null, // thumbnailUrl (별도 조회 필요)
                product.getPrice() == 0.0,
                product.getStockQuantity() != null,
                product.getStockQuantity() != null && product.getStockQuantity() == 0
        );
    }
}