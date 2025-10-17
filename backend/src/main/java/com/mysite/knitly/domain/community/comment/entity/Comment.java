package com.mysite.knitly.domain.community.comment.entity;

import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "comments")
@Where(clause = "is_deleted = false")
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Lob
    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    public void setPost(Post post) { this.post = post; }

    public void softDelete() { this.deleted = true; }

    public void update(String newContent) { this.content = newContent; }

    public boolean isAuthor(User user) {
        return user != null && author != null && author.getUserId().equals(user.getUserId());
    }
}
