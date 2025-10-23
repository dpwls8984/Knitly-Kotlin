package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상품 목록 조회용 DTO (대표 이미지 포함)
 * Native Query의 결과를 매핑
 */
@Getter
@AllArgsConstructor
public class ProductWithThumbnailDto {

    private Long productId;
    private String title;
    private ProductCategory productCategory;
    private Double price;
    private Integer purchaseCount;
    private Integer likeCount;
    private Integer stockQuantity;
    private Double avgReviewRating;
    private LocalDateTime createdAt;
    private String thumbnailUrl;  // 🔥 첫 번째 이미지 URL

    /**
     * ProductListResponse로 변환
     */
    public ProductListResponse toResponse() {
        return ProductListResponse.builder()
                .productId(productId)
                .title(title)
                .productCategory(productCategory)
                .price(price)
                .purchaseCount(purchaseCount)
                .likeCount(likeCount)
                .stockQuantity(stockQuantity)
                .avgReviewRating(avgReviewRating)
                .createdAt(createdAt)
                .thumbnailUrl(thumbnailUrl)
                .isFree(price == 0.0)
                .isLimited(stockQuantity != null)
                .isSoldOut(stockQuantity != null && stockQuantity == 0)
                .build();
    }
}