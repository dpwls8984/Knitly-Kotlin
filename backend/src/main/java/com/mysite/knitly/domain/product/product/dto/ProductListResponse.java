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
        Integer stockQuantity,
        Double avgReviewRating,
        LocalDateTime createdAt,
        Boolean isFree,
        Boolean isLimited,
        Boolean isSoldOut
) {
    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
                product.getProductId(),
                product.getTitle(),
                product.getProductCategory(),
                product.getPrice(),
                product.getPurchaseCount(),
                product.getLikeCount(),
                product.getStockQuantity(),
                product.getAvgReviewRating(),
                product.getCreatedAt(),
                product.getPrice() == 0.0,
                product.getStockQuantity() != null,
                product.getStockQuantity() != null && product.getStockQuantity() == 0
        );
    }
}