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

    // 다중 이미지 URL
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "post_images", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "url", nullable = false, length = 512)
    @OrderColumn(name = "sort_order")
    private List<String> imageUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "post_category", nullable = false, columnDefinition = "ENUM('FREE','QUESTION','TIP')")
    private PostCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
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

    // 이미지 수정
    public void update(String title, String content, PostCategory category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    // 이미지 교체
    public void replaceImages(List<String> newUrls) {
        this.imageUrls.clear();
        if (newUrls != null) {
            this.imageUrls.addAll(newUrls);
        }
    }

        public boolean isAuthor(User user) {
        return user != null && author != null && author.getUserId().equals(user.getUserId());
    }
}
