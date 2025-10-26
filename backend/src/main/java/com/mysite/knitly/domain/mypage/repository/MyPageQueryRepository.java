package com.mysite.knitly.domain.mypage.repository;

import com.mysite.knitly.domain.mypage.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class MyPageQueryRepository {

    @PersistenceContext
    private EntityManager em;

    // 주문 내역 조회 (카드 안에 묶음별로 표시)
    public Page<OrderCardResponse> findOrderCards(Long userId, Pageable pageable) {
        List<Long> orderIds = em.createQuery("""
                        select o.orderId from Order o
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
                               p.productId, p.title, oi.quantity, oi.orderPrice,
                               oi.orderItemId
                        from Order o
                        join o.orderItems oi
                        join oi.product p
                        where o.orderId in :ids
                        order by o.createdAt desc, oi.orderItemId asc
                        """, Object[].class)
                .setParameter("ids", orderIds)
                .getResultList();

        // 이 주문들에서 조회된 모든 상품 ID를 추출합니다.
        Set<Long> productIdsInOrders = rows.stream()
                .map(r -> (Long) r[3]) // rows에서 productId 추출
                .collect(Collectors.toSet());

        // 이 사용자가 리뷰를 작성한 Product ID 목록을 조회합니다.
        Set<Long> orderItemIds = rows.stream()
                .map(r -> (Long) r[7]) // rows[7]에서 orderItemId 추출
                .collect(Collectors.toSet());

        // 이 사용자가 리뷰를 작성한 'orderItemId' 목록을 조회합니다.
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

}
