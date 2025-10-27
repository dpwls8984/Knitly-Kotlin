package com.mysite.knitly.domain.product.product.dto;

import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;

import java.util.List;

public record ProductDetailResponse(
        Long productId,
        String title,
        String description,
        ProductCategory productCategory,
        String sizeInfo,
        Double price,
        String createdAt,
        Integer stockQuantity,

        Integer likeCount,

        boolean isLikedByUser,
        //Long designId
        Double avgReviewRating,
        List<String> productImageUrls,
        Integer reviewCount
) {
    public static ProductDetailResponse from(Product product, List<String> imageUrls, boolean isLikedByUser) {
        return new ProductDetailResponse(
                product.getProductId(),
                product.getTitle(),
                product.getDescription(),
                product.getProductCategory(),
                product.getSizeInfo(),
                product.getPrice(),
                product.getCreatedAt().toString(),
                product.getStockQuantity(),
                product.getLikeCount(),
                isLikedByUser,
                //product.getDesign().getDesignId()
                product.getAvgReviewRating(),
                imageUrls,
                product.getReviewCount() != null ? product.getReviewCount() : 0 // null 방지
        );
    }
}
