package com.mysite.knitly.domain.mypage.repository;

import com.mysite.knitly.domain.mypage.dto.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MyPageQueryRepository {

    private final EntityManager em;

    // 주문 내역 조회
    public Page<OrderCardResponse> findOrderCards(Long userId, Pageable pageable) {
        String jpql = """
            select o.orderId, o.createdAt, o.totalPrice,
                   p.productId, p.title,
                   oi.quantity, oi.orderPrice,
                   oi.orderItemId
            from Order o
            join o.orderItems oi
            join oi.product p
            where o.user.userId = :uid
            order by o.createdAt desc
        """;

        List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("uid", userId)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        long total = em.createQuery("select count(o) from Order o where o.user.userId = :uid", Long.class)
                .setParameter("uid", userId)
                .getSingleResult();

        if (rows.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        Set<Long> productIdsInOrders = rows.stream()
                .map(r -> (Long) r[3])
                .collect(Collectors.toSet());

        Set<Long> orderItemIds = rows.stream()
                .map(r -> (Long) r[7])
                .collect(Collectors.toSet());

        Set<Long> reviewedOrderItemIds = new HashSet<>(em.createQuery("""
                    select r.orderItem.orderItemId from Review r
                    where r.user.userId = :userId 
                      and r.orderItem.orderItemId in :orderItemIds
                    """, Long.class)
                .setParameter("userId", userId)
                .setParameter("orderItemIds", orderItemIds)
                .getResultList());

        Map<Long, LocalDateTime> orderedAtMap = new LinkedHashMap<>();
        Map<Long, Double> totalMap = new LinkedHashMap<>();
        Map<Long, List<OrderLine>> itemsMap = new LinkedHashMap<>();

        for (Object[] r : rows) {
            Long oId = (Long) r[0];
            LocalDateTime orderedAt = (LocalDateTime) r[1];
            Double totalPrice = (r[2] == null) ? 0d : ((Number) r[2]).doubleValue();
            Long productId = (Long) r[3];
            String productTitle = (String) r[4];
            Integer quantity = (Integer) r[5];
            Double orderPrice = (r[6] == null) ? 0d : ((Number) r[6]).doubleValue();
            Long orderItemId = (Long) r[7];

            orderedAtMap.putIfAbsent(oId, orderedAt);
            totalMap.putIfAbsent(oId, totalPrice);

            boolean isReviewed = reviewedOrderItemIds.contains(orderItemId);

            itemsMap.computeIfAbsent(oId, k -> new ArrayList<>())
                    .add(new OrderLine(orderItemId, productId, productTitle, quantity, orderPrice, isReviewed));
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

    // ✅ 수정: 내가 쓴 글 조회 - CAST를 사용하여 CLOB → VARCHAR 변환
    public Page<MyPostListItemResponse> findMyPosts(Long userId, String query, Pageable pageable) {
        String base = """
                SELECT new com.mysite.knitly.domain.mypage.dto.MyPostListItemResponse(
                    p.id,
                    p.title,
                    CASE WHEN LENGTH(CAST(p.content AS string)) > 10 
                         THEN CONCAT(SUBSTRING(CAST(p.content AS string), 1, 10), '...')
                         ELSE CAST(p.content AS string)
                    END,
                    (SELECT MIN(i) FROM Post p2 JOIN p2.imageUrls i WHERE p2 = p),
                    p.createdAt
                )
                FROM Post p
                WHERE p.author.userId = :uid
                  AND p.deleted = false
                """;

        if (query != null && !query.isBlank()) {
            base += " AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(CAST(p.content AS string)) LIKE LOWER(CONCAT('%', :q, '%')))";
        }

        var q = em.createQuery(base + " ORDER BY p.createdAt DESC", MyPostListItemResponse.class)
                .setParameter("uid", userId);

        if (query != null && !query.isBlank()) {
            q.setParameter("q", query.trim());
        }

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

    // ✅ 최종 수정: 내가 쓴 댓글 조회 - LocalDate로 CAST
    public Page<MyCommentListItem> findMyComments(Long userId, String query, Pageable pageable) {
        String base = """
                SELECT new com.mysite.knitly.domain.mypage.dto.MyCommentListItem(
                    c.id,
                    c.post.id,
                    CAST(c.createdAt AS LocalDate),
                    CASE WHEN LENGTH(CAST(c.content AS string)) > 30 
                         THEN CONCAT(SUBSTRING(CAST(c.content AS string), 1, 30), '...')
                         ELSE CAST(c.content AS string)
                    END
                )
                FROM Comment c
                WHERE c.author.userId = :uid
                  AND c.deleted = false
                """;

        if (query != null && !query.isBlank()) {
            base += " AND LOWER(CAST(c.content AS string)) LIKE LOWER(CONCAT('%', :q, '%'))";
        }

        var q = em.createQuery(base + " ORDER BY c.createdAt DESC", MyCommentListItem.class)
                .setParameter("uid", userId);

        if (query != null && !query.isBlank()) {
            q.setParameter("q", query.trim());
        }

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
}