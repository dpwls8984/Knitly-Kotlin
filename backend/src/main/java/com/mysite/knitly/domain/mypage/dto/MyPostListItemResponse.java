package com.mysite.knitly.domain.mypage.dto;

import java.time.LocalDateTime;

public record MyPostListItemResponse(
        Long id,
        String title,
        String excerpt,
        String thumbnailUrl,
        LocalDateTime createdAt
) {}
