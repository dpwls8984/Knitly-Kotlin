package com.mysite.knitly.domain.community.post.repository;

import com.mysite.knitly.domain.community.post.dto.PostListRowResponse;
import com.mysite.knitly.domain.community.post.entity.Post;
import com.mysite.knitly.domain.community.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "author")
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

    // 댓글 수 일괄 조회 (N+1 문제 제거)
    @Query("""
            SELECT c.post.id, COUNT(c.id)
            FROM Comment c
            WHERE c.post.id IN :postIds AND c.deleted = false
            GROUP BY c.post.id
            """)
              List<Object[]> countCommentsByPostIds(@Param("postIds") List<Long> postIds);

    //내가 쓴 글 목록 + 검색
    @Query("""
            select p
            from Post p
            where p.author.userId = :uid
             and (
                :q is null or :q = ''
                 or lower(p.title)   like lower(concat('%', :q, '%'))
                 or lower(cast(p.content as string)) like lower(concat('%', :q, '%'))
                  )
            """)
    Page<Post> findMyPosts(
            @Param("uid") Long userId,
            @Param("q") String query,
            Pageable pageable
    );

    long deleteByIdInAndAuthor_UserId(Collection<Long> ids, Long userId);
}