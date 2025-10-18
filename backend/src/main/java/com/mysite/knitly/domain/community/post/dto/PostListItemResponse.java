package com.mysite.knitly.domain.community.post.dto;

import com.mysite.knitly.domain.community.post.entity.PostCategory;
import java.time.LocalDateTime;

public record PostListItemResponse(

        Long id,
        PostCategory category,
        String title,
        String excerpt,
        String authorDisplay,
        LocalDateTime createdAt,
        Long commentCount,
        String thumbnailUrl
) {}