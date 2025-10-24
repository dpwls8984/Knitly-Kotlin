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
    public ProductListResponse toResponse(boolean isLikedByUser) {
        return new ProductListResponse(
                this.productId,
                this.title,
                this.productCategory,
                this.price,
                this.purchaseCount,
                this.likeCount,
                isLikedByUser,
                this.stockQuantity,
                this.avgReviewRating,
                this.createdAt,
                this.thumbnailUrl, // thumbnailUrl

                // 추가로 계산된 Boolean 필드들
                this.price == 0.0, // isFree
                this.stockQuantity != null, // isLimited
                this.stockQuantity != null && this.stockQuantity == 0 // isSoldOut
        );
    }
}