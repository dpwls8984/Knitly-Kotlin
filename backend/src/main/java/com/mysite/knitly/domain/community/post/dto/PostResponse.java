package com.mysite.knitly.domain.community.post.dto;

import com.mysite.knitly.domain.community.post.entity.PostCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

// 게시글 단건 응답 DTO
// mine: 사용자 = 작성자 여부 확인하고 버튼 제어
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PostResponse {
    private Long id;
    private PostCategory category;
    private String title;
    private String content;
    private String imageUrl;

    private UUID authorId;
    private String authorDisplay;   // 예: "익명의 털실-1234"

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long commentCount;
    private boolean mine;
}
