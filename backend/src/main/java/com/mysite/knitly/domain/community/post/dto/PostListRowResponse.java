package com.mysite.knitly.domain.community.post.dto;

import com.mysite.knitly.domain.community.post.entity.PostCategory;
import java.time.LocalDateTime;

public record PostListRowResponse(
        Long id,
        PostCategory category,
        String title,
        String excerpt,
        Long authorId,
        LocalDateTime createdAt,
        Long commentCount,
        String thumbnailUrl
) {}
