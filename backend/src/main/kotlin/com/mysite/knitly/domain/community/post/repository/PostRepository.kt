package com.mysite.knitly.domain.community.post.repository

import com.mysite.knitly.domain.community.post.entity.Post
import com.mysite.knitly.domain.community.post.entity.PostCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<Post, Long> {

    @Query(
        """
        SELECT p 
        FROM Post p 
        WHERE p.deleted = false
          AND (:category IS NULL OR p.category = :category)
          AND (
                :query IS NULL OR :query = '' 
                OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(CAST(p.content AS string)) LIKE LOWER(CONCAT('%', :query, '%'))
              )
        ORDER BY p.createdAt DESC
        """
    )
    fun findListRows(
        @Param("category") category: PostCategory?,
        @Param("query") query: String?,
        pageable: Pageable
    ): Page<Post>

    @Query(
        """
        SELECT COUNT(c.id) 
        FROM Comment c 
        WHERE c.post.id = :postId 
          AND c.deleted = false
        """
    )
    fun countCommentsByPostId(
        @Param("postId") postId: Long
    ): Long

    @Query(
        """
        SELECT c.post.id, COUNT(c.id)
        FROM Comment c
        WHERE c.post.id IN :postIds AND c.deleted = false
        GROUP BY c.post.id
        """
    )
    fun countCommentsByPostIds(
        @Param("postIds") postIds: List<Long>
    ): List<Array<Any>>

    @Query(
        """
        SELECT p
        FROM Post p
        WHERE p.author.userId = :uid
          AND p.deleted = false
          AND (
                :q IS NULL OR :q = '' 
                OR LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(CAST(p.content AS string)) LIKE LOWER(CONCAT('%', :q, '%'))
              )
        """
    )
    fun findMyPosts(
        @Param("uid") userId: Long,
        @Param("q") query: String?,
        pageable: Pageable
    ): Page<Post>

    fun deleteByIdInAndAuthor_UserId(
        ids: Collection<Long>,
        userId: Long
    ): Long
}
