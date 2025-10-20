package com.mysite.knitly.domain.community.comment.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String content,
        Long authorId,
        String authorDisplay,
        LocalDateTime createdAt,
        boolean mine
) {}
