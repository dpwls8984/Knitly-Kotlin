package com.mysite.knitly.domain.community.comment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        Long id,
        String content,
        UUID authorId,
        String authorDisplay,
        LocalDateTime createdAt,
        boolean mine
) {}
