package com.mysite.knitly.domain.home.dto;

import java.time.LocalDateTime;

public record LatestPostItem(
        Long postId,
        String title,
        String category,
        String thumbnailUrl,
        LocalDateTime createdAt
) {}
