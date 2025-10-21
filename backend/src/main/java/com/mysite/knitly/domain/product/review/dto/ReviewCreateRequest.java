package com.mysite.knitly.domain.product.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record ReviewCreateRequest(
        @NotNull(message = "리뷰 점수는 필수입니다.")
        @Min(value = 1, message = "평점은 1~5 사이여야 합니다.")
        @Max(value = 5, message = "평점은 1~5 사이여야 합니다.")
        Integer rating,

        @NotNull(message = "리뷰 내용은 필수입니다.")
        String content,

        @Size(max = 10, message = "리뷰 이미지는 최대 10개까지 등록할 수 있습니다.")
        List<MultipartFile> reviewImageUrls
) {}
