package com.mysite.knitly.domain.community.post.repository;

import com.mysite.knitly.domain.community.post.dto.PostListRow;
import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(
            "SELECT new com.mysite.knitly.domain.community.post.dto.PostListRow(" +
                    "  p.id, " +
                    "  p.category, " +
                    "  p.title, " +
                    "  p.content, " +
                    "  p.author.userId, " +
                    "  p.createdAt, " +
                    "  (SELECT COUNT(c.id) FROM Comment c WHERE c.post.id = p.id AND c.deleted = false), " +
                    "  p.imageUrl" +
                    ") " +
                    "FROM Post p " +
                    "WHERE (:category IS NULL OR p.category = :category) " +
                    "ORDER BY p.createdAt DESC"
    )
    Page<PostListRow> findListRows(@Param("category") PostCategory category, Pageable pageable);
    @Query("SELECT COUNT(c.id) FROM Comment c WHERE c.post.id = :postId AND c.deleted = false")
    long countCommentsByPostId(@Param("postId") Long postId);
}
