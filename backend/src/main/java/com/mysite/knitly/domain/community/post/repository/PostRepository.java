package com.mysite.knitly.domain.community.post.repository;

import com.mysite.knitly.domain.community.post.dto.PostListRowResponse;
import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query(
            "SELECT p " +
                    "FROM Post p " +
                    "WHERE (:category IS NULL OR p.category = :category) " +
                    "  AND ( :query IS NULL OR :query = '' " +
                    "        OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "        OR p.content      LIKE CONCAT('%', :query, '%') ) " +
                    "ORDER BY p.createdAt DESC"
    )
    Page<Post> findListRows(@Param("category") PostCategory category,
                            @Param("query") String query,
                            Pageable pageable);

    @Query("SELECT COUNT(c.id) FROM Comment c WHERE c.post.id = :postId AND c.deleted = false")
    long countCommentsByPostId(@Param("postId") Long postId);
}
