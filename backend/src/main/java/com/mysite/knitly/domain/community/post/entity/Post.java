package com.mysite.knitly.domain.community.post.entity;

import com.mysite.knitly.domain.community.comment.entity.Comment;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "posts")
@Where(clause = "is_deleted = false")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

   // 이미지 추가 URL (URL만 저장)
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_category", nullable = false, columnDefinition = "ENUM('FREE','QUESTION','TIP')")
    private PostCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private User author;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setPost(this);
    }

    public void softDelete() { this.deleted = true; }

    // 이미지 수정 시
    public void update(String title, String content, String imageUrl, PostCategory category) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public boolean isAuthor(User user) {
        return user != null && author != null && author.getUserId().equals(user.getUserId());
    }
}
