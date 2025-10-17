package com.mysite.knitly.domain.community.post.dto;

import com.mysite.knitly.domain.community.post.entity.PostCategory;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor   // Builder 오류가 때문에 생성자 추가
@Builder
public class PostListItem {

    private Long id;
    private PostCategory category;   // 카테고리 (FREE / QUESTION / TIP)
    private String title;
    private String excerpt;          // 내용 요약
    private String authorDisplay;    // 익명 표시
    private LocalDateTime createdAt;
    private Long commentCount;       // 댓글 수
    private String thumbnailUrl;
}
