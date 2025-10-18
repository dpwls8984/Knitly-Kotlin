package com.mysite.knitly.domain.community.post.dto;

import com.mysite.knitly.domain.community.post.entity.PostCategory;
import java.time.LocalDateTime;
import java.util.UUID;

public record PostListRowResponse(
        Long id,
        PostCategory category,
        String title,
        String excerpt,
        UUID authorId,
        LocalDateTime createdAt,
        Long commentCount,
        String thumbnailUrl
) {}
