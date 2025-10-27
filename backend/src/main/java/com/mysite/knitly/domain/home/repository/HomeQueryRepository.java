package com.mysite.knitly.domain.home.repository;

import com.mysite.knitly.domain.home.dto.LatestPostItem;
import com.mysite.knitly.domain.home.dto.LatestReviewItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class HomeQueryRepository {

    @PersistenceContext
    private EntityManager em;

    // 최신 리뷰 N개
    public List<LatestReviewItem> findLatestReviews(int limit) {
        return em.createQuery("""
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
                """, LatestReviewItem.class)
                .setMaxResults(limit)
                .getResultList();
    }

    // 최신 커뮤니티 글 N개 (deleted=false, 최신순)
    public List<LatestPostItem> findLatestPosts(int limit) {
        return em.createQuery("""
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
                """, LatestPostItem.class)
                .setMaxResults(limit)
                .getResultList();
    }
}