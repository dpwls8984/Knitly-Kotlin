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
        String thumbnailUrl,// 대표 이미지 URL (sortOrder = 1)
        String sellerName,
        Boolean isFree,     // 무료 여부
        Boolean isLimited,  // 한정판매 여부
        Boolean isSoldOut   // 품절 여부 (stockQuantity = 0)
) {
    public static ProductListResponse from(Product product, boolean isLikedByUser) {
        // Product의 첫 번째 이미지를 thumbnailUrl로 사용
        String thumbnailUrl = null;
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            // sortOrder가 1인 이미지를 찾거나, 없으면 첫 번째 이미지 사용
            thumbnailUrl = product.getProductImages().stream()
                    .filter(img -> img.getSortOrder() != null && img.getSortOrder() == 1L)
                    .findFirst()
                    .map(img -> img.getProductImageUrl())
                    .orElseGet(() -> product.getProductImages().get(0).getProductImageUrl());
        }

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
                thumbnailUrl, // 🔥 수정: Product의 첫 번째 이미지 URL
                product.getUser() !=null? product.getUser().getName() : "알 수 없음",
                product.getPrice() == 0.0,
                product.getStockQuantity() != null,
                product.getStockQuantity() != null && product.getStockQuantity() == 0
        );
    }
}