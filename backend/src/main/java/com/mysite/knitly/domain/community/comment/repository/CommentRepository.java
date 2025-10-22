package com.mysite.knitly.domain.community.comment.repository;

import com.mysite.knitly.domain.community.comment.entity.Comment;
import com.mysite.knitly.domain.community.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPost(Post post);

    // 정렬（등록순,최신순)
    @EntityGraph(attributePaths = "author")
    Page<Comment> findByPostAndDeletedFalseOrderByCreatedAtAsc(Post post, Pageable pageable);
    @EntityGraph(attributePaths = "author")
    Page<Comment> findByPostAndDeletedFalseOrderByCreatedAtDesc(Post post, Pageable pageable);

    // 댓글 수
    long countByPostIdAndDeletedFalse(Long postId);

    // 루트 댓글
    @EntityGraph(attributePaths = "author")
    Page<Comment> findByPostAndParentIsNullAndDeletedFalseOrderByCreatedAtAsc(Post post, Pageable pageable);
    @EntityGraph(attributePaths = "author")
    Page<Comment> findByPostAndParentIsNullAndDeletedFalseOrderByCreatedAtDesc(Post post, Pageable pageable);

    // 자식 대댓글
    @EntityGraph(attributePaths = "author")
    List<Comment> findByParentIdAndDeletedFalseOrderByCreatedAtAsc(Long parentId);

    // 자식 대댓글 배치 조회 (N+1 제거)
    @EntityGraph(attributePaths = "author")
    List<Comment> findByParentIdInAndDeletedFalseOrderByCreatedAtAsc(Collection<Long> parentIds);

    // 마이페이지 댓글 조회 시
    Page<Comment> findByAuthor_UserIdAndDeletedFalseAndContentContainingIgnoreCaseOrderByCreatedAtDesc(
            Long userId, String content, Pageable pageable
    );

    @Query("SELECT c.author.userId " +
            "FROM Comment c " +
            "WHERE c.deleted = false AND c.post.id = :postId " +
            "GROUP BY c.author.userId " +
            "ORDER BY MIN(c.createdAt) ASC")

    List<Long> findAuthorOrderForPost(@Param("postId") Long postId);
}
