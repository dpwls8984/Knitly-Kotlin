package com.mysite.knitly.domain.community.post.dto;

import com.mysite.knitly.domain.community.post.entity.PostCategory;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

// 게시글 단건 응답 DTO
// mine: 사용자 = 작성자 여부 확인하고 버튼 제어
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