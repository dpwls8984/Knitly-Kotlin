package com.mysite.knitly.domain.community.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

        //  트리 응답 구조
        public record CommentTreeResponse(
                Long id,
                String content,
                Long authorId,
                String authorDisplay,
                LocalDateTime createdAt,
                boolean mine,
                Long parentId,
                List<CommentTreeResponse> children
) {}
