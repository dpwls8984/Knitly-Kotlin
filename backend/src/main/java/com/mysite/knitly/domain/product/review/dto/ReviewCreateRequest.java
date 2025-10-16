package com.mysite.knitly.domain.product.review.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record ReviewCreateRequest(
        Byte rating, // 1~5 점수
        String content,
        List<MultipartFile> reviewImageUrls
) {}
