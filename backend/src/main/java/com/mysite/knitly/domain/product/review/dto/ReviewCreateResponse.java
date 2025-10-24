package com.mysite.knitly.domain.product.review.dto;

public record ReviewCreateResponse(
        String productTitle,
        String productThumbnailUrl
) {}