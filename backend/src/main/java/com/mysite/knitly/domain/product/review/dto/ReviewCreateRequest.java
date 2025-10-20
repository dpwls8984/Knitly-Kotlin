package com.mysite.knitly.domain.product.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record ReviewCreateRequest(
        @NotNull(message = "REVIEW_RATING_INVALID")
        @Min(value = 1, message = "REVIEW_RATING_INVALID")
        @Max(value = 5, message = "REVIEW_RATING_INVALID")
        Integer rating, // 1~5 점수

        String content,

        @Size(max = 10, message = "REVIEW_IMAGES_TOO_MANY")
        List<MultipartFile> reviewImageUrls
) {}
