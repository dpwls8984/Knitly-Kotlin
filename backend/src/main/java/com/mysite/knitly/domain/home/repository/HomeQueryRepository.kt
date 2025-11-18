package com.mysite.knitly.domain.home.repository

import com.mysite.knitly.domain.home.dto.LatestPostItem
import com.mysite.knitly.domain.home.dto.LatestReviewItem
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

@Repository
class HomeQueryRepository(

    @PersistenceContext
    private val em: EntityManager

) {

    // 최신 리뷰 N개
    fun findLatestReviews(limit: Int): List<LatestReviewItem> {
        return em.createQuery(
            """
                SELECT new com.mysite.knitly.domain.home.dto.LatestReviewItem(
                    r.reviewId,
                    p.productId,
                    p.title,
                    CAST(NULL AS string),
                    r.rating,
                    r.content,
                    CAST(r.createdAt AS LocalDate)
                )
                FROM Review r
                JOIN r.product p
                WHERE r.isDeleted = false
                ORDER BY r.createdAt DESC
            """.trimIndent(),
            LatestReviewItem::class.java
        )
            .setMaxResults(limit)
            .resultList
    }

    // 최신 커뮤니티 글 N개 (deleted=false, 최신순)
    fun findLatestPosts(limit: Int): List<LatestPostItem> {
        return em.createQuery(
            """
                SELECT new com.mysite.knitly.domain.home.dto.LatestPostItem(
                    p.id,
                    p.title,
                    CAST(p.category AS string),
                    CAST(NULL AS string),
                    p.createdAt
                )
                FROM Post p
                WHERE p.deleted = false
                ORDER BY p.createdAt DESC
            """.trimIndent(),
            LatestPostItem::class.java
        )
            .setMaxResults(limit)
            .resultList
    }
}