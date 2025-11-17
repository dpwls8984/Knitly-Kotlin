package com.mysite.knitly.domain.community.comment.repository

import com.mysite.knitly.domain.community.comment.entity.Comment
import com.mysite.knitly.domain.community.post.entity.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<Comment, Long> {

    fun findByPost(post: Post): List<Comment>

    fun findByPostAndDeletedFalseOrderByCreatedAtAsc(post: Post, pageable: Pageable): Page<Comment>
    fun findByPostAndDeletedFalseOrderByCreatedAtDesc(post: Post, pageable: Pageable): Page<Comment>

    fun countByPostIdAndDeletedFalse(postId: Long): Long

    fun findByPostAndParentIsNullAndDeletedFalseOrderByCreatedAtAsc(post: Post, pageable: Pageable): Page<Comment>
    fun findByPostAndParentIsNullAndDeletedFalseOrderByCreatedAtDesc(post: Post, pageable: Pageable): Page<Comment>

    fun findByParentIdAndDeletedFalseOrderByCreatedAtAsc(parentId: Long): List<Comment>

    fun findByParentIdInAndDeletedFalseOrderByCreatedAtAsc(parentIds: Collection<Long>): List<Comment>

    fun findByAuthor_UserIdAndDeletedFalseAndContentContainingIgnoreCaseOrderByCreatedAtDesc(
        userId: Long,
        content: String,
        pageable: Pageable
    ): Page<Comment>

    @Query(
        "SELECT c.author.userId " +
                "FROM Comment c " +
                "WHERE c.deleted = false AND c.post.id = :postId " +
                "GROUP BY c.author.userId " +
                "ORDER BY MIN(c.createdAt) ASC"
    )
    fun findAuthorOrderForPost(@Param("postId") postId: Long): List<Long>

    fun findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId: Long): List<Comment>
}
