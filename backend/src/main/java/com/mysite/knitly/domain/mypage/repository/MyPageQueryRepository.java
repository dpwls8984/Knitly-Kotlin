package com.mysite.knitly.domain.mypage.repository;

import com.mysite.knitly.domain.mypage.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class MyPageQueryRepository {

    @PersistenceContext
    private EntityManager em;

    // 주문 내역 조회 (카드 안에 상품 묶음)
    public Page<OrderCardResponse> findOrderCards(Long userId, Pageable pageable) {
        List<Long> orderIds = em.createQuery("""
                        select o.orderId
                        from Order o
                        where o.user.userId = :uid
                        order by o.createdAt desc
                        """, Long.class)
                .setParameter("uid", userId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        if (orderIds.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);

        Long total = em.createQuery("""
                        select count(o.orderId)
                        from Order o
                        where o.user.userId = :uid
                        """, Long.class)
                .setParameter("uid", userId)
                .getSingleResult();

        List<Object[]> rows = em.createQuery("""
                        select o.orderId, o.createdAt, o.totalPrice,
                               p.productId, p.title, oi.quantity, oi.orderPrice
                        from Order o
                        join o.orderItems oi
                        join oi.product p
                        where o.orderId in :ids
                        order by o.createdAt desc, oi.orderItemId asc
                        """, Object[].class)
                .setParameter("ids", orderIds)
                .getResultList();

        // 불변 DTO 조립
        Map<Long, LocalDateTime> orderedAtMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> totalMap = new LinkedHashMap<>();
        Map<Long, List<OrderLine>> itemsMap = new LinkedHashMap<>();

        for (Object[] r : rows) {
            Long oId = (Long) r[0];
            LocalDateTime orderedAt = (LocalDateTime) r[1];
            BigDecimal totalPrice = (r[2] == null) ? BigDecimal.ZERO : (BigDecimal) r[2];
            Long productId = (Long) r[3];
            String productTitle = (String) r[4];
            Integer quantity = (Integer) r[5];
            BigDecimal orderPrice = (r[6] == null) ? BigDecimal.ZERO : (BigDecimal) r[6];

            orderedAtMap.putIfAbsent(oId, orderedAt);
            totalMap.putIfAbsent(oId, totalPrice);
            itemsMap.computeIfAbsent(oId, k -> new ArrayList<>())
                    .add(new OrderLine(productId, productTitle, quantity, orderPrice));
        }

        List<OrderCardResponse> cards = new ArrayList<>();
        for (Long id : itemsMap.keySet()) {
            cards.add(new OrderCardResponse(
                    id,
                    orderedAtMap.get(id),
                    totalMap.get(id),
                    Collections.unmodifiableList(itemsMap.get(id))
            ));
        }

        return new PageImpl<>(cards, pageable, total);
    }

    // 내가 쓴 글 조회 (검색 + 요약표시)
    public Page<MyPostListItemResponse> findMyPosts(Long userId, String query, Pageable pageable) {
        String base = """
                SELECT new com.mysite.knitly.domain.mypage.dto.MyPostListItemResponse(
                    p.id,
                    p.title,
                    SUBSTRING(p.content, 1, 10),
                    /* ElementCollection은 LIMIT 불가 → 대표값으로 MIN(i) 사용 */
                    (SELECT MIN(i) FROM Post p2 JOIN p2.imageUrls i WHERE p2 = p),
                    p.createdAt
                )
                FROM Post p
                WHERE p.author.userId = :uid
                  AND p.deleted = false
                """;
        if (query != null && !query.isBlank()) {
            base += " AND LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))";
        }

        var q = em.createQuery(base + " ORDER BY p.createdAt DESC", MyPostListItemResponse.class)
                .setParameter("uid", userId);
        if (query != null && !query.isBlank()) q.setParameter("q", query.trim());

        List<MyPostListItemResponse> list = q
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        long total = em.createQuery("""
                        SELECT COUNT(p.id)
                        FROM Post p
                        WHERE p.author.userId = :uid AND p.deleted = false
                        """, Long.class)
                .setParameter("uid", userId)
                .getSingleResult();

        return new PageImpl<>(list, pageable, total);
    }

    // 내가 쓴 댓글 조회 (검색 + 요약표시)
    public Page<MyCommentListItem> findMyComments(Long userId, String query, Pageable pageable) {
        String base = """
                SELECT new com.mysite.knitly.domain.mypage.dto.MyCommentListItem(
                    c.id,
                    c.post.id,
                    FUNCTION('DATE', c.createdAt),
                    SUBSTRING(c.content, 1, 30)
                )
                FROM Comment c
                WHERE c.author.userId = :uid
                  AND c.deleted = false
                """;
        if (query != null && !query.isBlank()) {
            base += " AND LOWER(c.content) LIKE LOWER(CONCAT('%', :q, '%'))";
        }

        var q = em.createQuery(base + " ORDER BY c.createdAt DESC", MyCommentListItem.class)
                .setParameter("uid", userId);
        if (query != null && !query.isBlank()) q.setParameter("q", query.trim());

        List<MyCommentListItem> list = q
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        long total = em.createQuery("""
                        SELECT COUNT(c.id)
                        FROM Comment c
                        WHERE c.author.userId = :uid AND c.deleted = false
                        """, Long.class)
                .setParameter("uid", userId)
                .getSingleResult();

        return new PageImpl<>(list, pageable, total);
    }

    // 내가 찜한 상품 조회
    public Page<FavoriteProductItem> findMyFavoriteProducts(Long userId, Pageable pageable) {
        String jpql = """
                SELECT new com.mysite.knitly.domain.mypage.dto.FavoriteProductItem(
                    p.productId,
                    p.title,
                    p.thumbnailUrl,
                    p.price,
                    COALESCE(AVG(r.rating), 0),
                    FUNCTION('DATE', pl.createdAt)
                )
                FROM ProductLike pl
                JOIN pl.product p
                LEFT JOIN Review r ON r.product = p
                WHERE pl.user.userId = :uid
                GROUP BY p.productId, p.title, p.thumbnailUrl, p.price, pl.createdAt
                ORDER BY pl.createdAt DESC
                """;

        var query = em.createQuery(jpql, FavoriteProductItem.class)
                .setParameter("uid", userId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());

        var list = query.getResultList();

        long total = em.createQuery("""
                        SELECT COUNT(pl)
                        FROM ProductLike pl
                        WHERE pl.user.userId = :uid
                        """, Long.class)
                .setParameter("uid", userId)
                .getSingleResult();

        return new PageImpl<>(list, pageable, total);
    }

    // 리뷰 조회 (리뷰 이미지 + 상품 첫번째 이미지 포함)
    public Page<ReviewListItem> findMyReviews(Long userId, Pageable pageable) {
        long total = em.createQuery("""
                        SELECT COUNT(r.reviewId)
                        FROM Review r
                        WHERE r.user.userId = :uid AND r.isDeleted = false
                        """, Long.class)
                .setParameter("uid", userId)
                .getSingleResult();

        if (total == 0L) return new PageImpl<>(List.of(), pageable, 0L);

        List<Object[]> rows = em.createQuery("""
                        SELECT r.reviewId,
                               p.productId,
                               p.title,
                               (SELECT MIN(pi.productImageUrl)
                                FROM ProductImage pi
                                WHERE pi.product = p),
                               r.rating,
                               r.content,
                               FUNCTION('DATE', r.createdAt),
                               (SELECT FUNCTION('DATE', MAX(o.createdAt))
                                FROM Order o JOIN o.orderItems oi
                                WHERE o.user.userId = :uid AND oi.product = p)
                        FROM Review r
                        JOIN r.product p
                        WHERE r.user.userId = :uid
                          AND r.isDeleted = false
                        ORDER BY r.createdAt DESC
                        """, Object[].class)
                .setParameter("uid", userId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // 리뷰 이미지 전체 조회
        List<Long> ids = rows.stream().map(r -> (Long) r[0]).toList();
        Map<Long, List<String>> imageMap = new LinkedHashMap<>();
        em.createQuery("""
                        SELECT ri.review.reviewId, ri.reviewImageUrl
                        FROM ReviewImage ri
                        WHERE ri.review.reviewId IN :ids
                        ORDER BY ri.sortOrder ASC, ri.reviewImageId ASC
                        """, Object[].class)
                .setParameter("ids", ids)
                .getResultList()
                .forEach(r -> imageMap
                        .computeIfAbsent((Long) r[0], k -> new ArrayList<>())
                        .add((String) r[1]));

        // DTO 매핑
        List<ReviewListItem> list = rows.stream().map(r ->
                new ReviewListItem(
                        (Long) r[0],                 // reviewId
                        (Long) r[1],                 // productId
                        (String) r[2],               // productTitle
                        (String) r[3],               // product thumbnail (first)
                        (Integer) r[4],              // rating
                        (String) r[5],               // content
                        imageMap.getOrDefault(r[0], List.of()), // review images
                        (java.time.LocalDate) r[6],  // createdDate
                        (java.time.LocalDate) r[7]   // purchasedDate
                )
        ).toList();

        return new PageImpl<>(list, pageable, total);
    }
}
