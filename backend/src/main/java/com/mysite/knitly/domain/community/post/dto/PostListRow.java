package com.mysite.knitly.domain.community.post.dto;

import com.mysite.knitly.domain.community.post.entity.PostCategory;

import java.time.LocalDateTime;
import java.util.UUID;

public class PostListRow {
    public final Long id;                  // 게시글 PK
    public final PostCategory category;
    public final String title;
    public final String excerpt;
    public final UUID authorId;            // 작성자 UUID (익명표기)
    public final LocalDateTime createdAt;  // 생성시각
    public final Long commentCount;        // 댓글 수
    public final String imageUrl;          // 이미지 URL

    public PostListRow(Long id,
                       PostCategory category,
                       String title,
                       String excerpt,
                       UUID authorId,
                       LocalDateTime createdAt,
                       Long commentCount,
                       String imageUrl) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.excerpt = excerpt;
        this.authorId = authorId;
        this.createdAt = createdAt;
        this.commentCount = commentCount;
        this.imageUrl = imageUrl;
    }
}
