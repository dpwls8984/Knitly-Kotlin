package com.mysite.knitly.domain.community.comment.repository;

import com.mysite.knitly.domain.community.comment.entity.Comment;
import com.mysite.knitly.domain.community.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPost(Post post);

    // 정렬（등록순,최신순)
    Page<Comment> findByPostAndDeletedFalseOrderByCreatedAtAsc(Post post, Pageable pageable);
    Page<Comment> findByPostAndDeletedFalseOrderByCreatedAtDesc(Post post, Pageable pageable);

    // 댓글 수
    long countByPostIdAndDeletedFalse(Long postId);

    // 루트 댓글
    Page<Comment> findByPostAndParentIsNullAndDeletedFalseOrderByCreatedAtAsc(Post post, Pageable pageable);
    Page<Comment> findByPostAndParentIsNullAndDeletedFalseOrderByCreatedAtDesc(Post post, Pageable pageable);

    // 자식 대댓글
    List<Comment> findByParentIdAndDeletedFalseOrderByCreatedAtAsc(Long parentId);

    // 익명의 털실 작성자 순서
    @Query("""
        SELECT c.author.userId
        FROM Comment c
        WHERE c.deleted = false AND c.post.id = :postId
        GROUP BY c.author.userId
        ORDER BY MIN(c.createdAt) ASC
    """)

    List<Long> findAuthorOrderForPost(@Param("postId") Long postId);
}
