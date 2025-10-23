package com.mysite.knitly.domain.community.post.dto;

import com.mysite.knitly.domain.community.post.entity.PostCategory;
import java.time.LocalDateTime;
import java.util.List;

// 게시글 단건 응답 DTO
public record PostResponse(
    Long id,
    PostCategory category,
    String title,
    String content,
    List<String> imageUrls,
    Long authorId,
    String authorDisplay,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long commentCount,
    boolean mine
) {}