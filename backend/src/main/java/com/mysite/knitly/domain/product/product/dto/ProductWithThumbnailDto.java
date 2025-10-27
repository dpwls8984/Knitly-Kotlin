package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상품 목록 조회용 DTO (대표 이미지 포함)
 * Native Query의 결과를 매핑
 */
public record ProductWithThumbnailDto (Long productId,
                                       String title,
                                       ProductCategory productCategory,
                                       Double price,
                                       Integer purchaseCount,
                                       Integer likeCount,
                                       Integer stockQuantity,
                                       Double avgReviewRating,
                                       LocalDateTime createdAt,
                                       String thumbnailUrl){



    /**
     * ProductListResponse로 변환
     */
    public ProductListResponse toResponse(boolean isLikedByUser, String sellerName) {
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
                sellerName,
                // 추가로 계산된 Boolean 필드들
                this.price == 0.0, // isFree
                this.stockQuantity != null, // isLimited
                this.stockQuantity != null && this.stockQuantity == 0 // isSoldOut
        );
    }
}