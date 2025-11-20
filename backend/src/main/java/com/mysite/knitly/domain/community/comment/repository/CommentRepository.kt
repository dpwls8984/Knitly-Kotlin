package com.mysite.knitly.domain.community.comment.repository

import com.mysite.knitly.domain.community.comment.entity.Comment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<Comment, Long> {

    // 게시글별 댓글 개수 조회
    fun countByPostIdAndDeletedFalse(postId: Long): Long

    // 특정 부모 댓글의 자식 댓글 조회 (대댓글)
    fun findByParentIdAndDeletedFalseOrderByCreatedAtAsc(parentId: Long): List<Comment>

    // 여러 부모 댓글의 자식 댓글 한 번에 조회
    fun findByParentIdInAndDeletedFalseOrderByCreatedAtAsc(parentIds: Collection<Long>): List<Comment>

    @Query(
        "SELECT c.author.userId " +
                "FROM Comment c " +
                "WHERE c.deleted = false AND c.post.id = :postId " +
                "GROUP BY c.author.userId " +
                "ORDER BY MIN(c.createdAt) ASC"
    )
    fun findAuthorOrderForPost(@Param("postId") postId: Long): List<Long>

    // 게시글의 전체 댓글 목록
    fun findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId: Long): List<Comment>
}
