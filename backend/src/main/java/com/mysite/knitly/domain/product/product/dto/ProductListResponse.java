package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductListResponse {

    private Long productId;
    private String title;
    private ProductCategory productCategory;
    private Double price;
    private Integer purchaseCount;
    private Integer likeCount;
    private Integer stockQuantity;
    private Double avgReviewRating;
    private LocalDateTime createdAt;

    // 🔥 대표 이미지 URL (sortOrder = 1)
    private String thumbnailUrl;

    // 추가 정보
    private Boolean isFree;         // 무료 여부
    private Boolean isLimited;      // 한정판매 여부
    private Boolean isSoldOut;      // 품절 여부 (stockQuantity = 0)

    public static ProductListResponse from(Product product) {
        return ProductListResponse.builder()
                .productId(product.getProductId())
                .title(product.getTitle())
                .productCategory(product.getProductCategory())
                .price(product.getPrice())
                .purchaseCount(product.getPurchaseCount())
                .likeCount(product.getLikeCount())
                .stockQuantity(product.getStockQuantity())
                .avgReviewRating(product.getAvgReviewRating())
                .createdAt(product.getCreatedAt())
                .thumbnailUrl(null) // 별도 조회 필요
                .isFree(product.getPrice() == 0.0)
                .isLimited(product.getStockQuantity() != null)
                .isSoldOut(product.getStockQuantity() != null && product.getStockQuantity() == 0)
                .build();
    }
}
