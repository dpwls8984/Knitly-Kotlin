package com.mysite.knitly.domain.mypage.repository

import com.mysite.knitly.domain.mypage.dto.MyCommentListItem
import com.mysite.knitly.domain.mypage.dto.MyPostListItemResponse
import com.mysite.knitly.domain.mypage.dto.OrderCardResponse
import com.mysite.knitly.domain.mypage.dto.OrderLine
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MyPageQueryRepository(
    private val em: EntityManager
) {

    fun findOrderCards(userId: Long, pageable: Pageable): Page<OrderCardResponse> {
        val jpql = """
            select o.orderId, o.createdAt, o.totalPrice,
                   p.productId, p.title,
                   oi.quantity, oi.orderPrice,
                   oi.orderItemId
            from Order o
            join o.orderItems oi
            join oi.product p
            left join Payment pay on pay.order.orderId = o.orderId
            where o.user.userId = :uid
            and pay.paymentStatus = 'DONE'
            order by o.createdAt desc
        """.trimIndent()

        val rows: List<Array<Any>> = em.createQuery(jpql, Array<Any>::class.java)
            .setParameter("uid", userId)
            .setFirstResult(pageable.offset.toInt())
            .setMaxResults(pageable.pageSize)
            .resultList

        val total: Long = em.createQuery(
            "select count(o) from Order o where o.user.userId = :uid",
            java.lang.Long::class.java
        )
            .setParameter("uid", userId)
            .singleResult
            .toLong()

        if (rows.isEmpty()) {
            return PageImpl(emptyList(), pageable, total)
        }

        val productIdsInOrders: Set<Long?> = rows
            .map { it[3] as Long? }
            .toSet()

        val orderItemIds: Set<Long> = rows
            .map { (it[7] as Number).toLong() }
            .toSet()

        val reviewedOrderItemIds: Set<Long> =
            em.createQuery(
                """
                    select r.orderItem.orderItemId from Review r
                    where r.user.userId = :userId 
                      and r.orderItem.orderItemId in :orderItemIds
                """.trimIndent(),
                java.lang.Long::class.java
            )
                .setParameter("userId", userId)
                .setParameter("orderItemIds", orderItemIds)
                .resultList
                .map { it.toLong() }
                .toSet()

        val orderedAtMap = LinkedHashMap<Long, LocalDateTime>()
        val totalMap = LinkedHashMap<Long, Double>()
        val itemsMap = LinkedHashMap<Long, MutableList<OrderLine>>()

        for (r in rows) {
            val oId = (r[0] as Number).toLong()
            val orderedAt = r[1] as LocalDateTime
            val totalPrice = (r[2] as? Number)?.toDouble() ?: 0.0
            val productId = (r[3] as Number).toLong()
            val productTitle = r[4] as String
            val quantity = (r[5] as Number).toInt()
            val orderPrice = (r[6] as? Number)?.toDouble() ?: 0.0
            val orderItemId = (r[7] as Number).toLong()

            orderedAtMap.putIfAbsent(oId, orderedAt)
            totalMap.putIfAbsent(oId, totalPrice)

            val isReviewed = reviewedOrderItemIds.contains(orderItemId)

            val lines = itemsMap.getOrPut(oId) { mutableListOf() }
            lines.add(
                OrderLine(
                    orderItemId = orderItemId,
                    productId = productId,
                    productTitle = productTitle,
                    quantity = quantity,
                    orderPrice = orderPrice,
                    isReviewed = isReviewed
                )
            )
        }

        val cards: List<OrderCardResponse> = itemsMap.keys.map { id ->
            OrderCardResponse(
                orderId = id,
                orderedAt = orderedAtMap[id]!!,
                totalPrice = totalMap[id]!!,
                items = itemsMap[id]!!.toList()
            )
        }

        return PageImpl(cards, pageable, total)
    }

    fun findMyPosts(userId: Long, query: String?, pageable: Pageable): Page<MyPostListItemResponse> {
        var base = """
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
                """.trimIndent()

        if (query != null && query.isNotBlank()) {
            base += " AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(CAST(p.content AS string)) LIKE LOWER(CONCAT('%', :q, '%')))"
        }

        val q = em.createQuery("$base ORDER BY p.createdAt DESC", MyPostListItemResponse::class.java)
            .setParameter("uid", userId)

        if (query != null && query.isNotBlank()) {
            q.setParameter("q", query.trim())
        }

        val list: List<MyPostListItemResponse> = q
            .setFirstResult(pageable.offset.toInt())
            .setMaxResults(pageable.pageSize)
            .resultList

        val total: Long = em.createQuery(
            """
                        SELECT COUNT(p.id)
                        FROM Post p
                        WHERE p.author.userId = :uid AND p.deleted = false
            """.trimIndent(),
            java.lang.Long::class.java
        )
            .setParameter("uid", userId)
            .singleResult
            .toLong()

        return PageImpl(list, pageable, total)
    }

    fun findMyComments(userId: Long, query: String?, pageable: Pageable): Page<MyCommentListItem> {
        var base = """
                SELECT new com.mysite.knitly.domain.mypage.dto.MyCommentListItem(
                    c.id,
                    c.post.id,
                    c.createdAt,
                    CASE WHEN LENGTH(CAST(c.content AS string)) > 30
                         THEN CONCAT(SUBSTRING(CAST(c.content AS string), 1, 30), '...')
                         ELSE CAST(c.content AS string)
                    END
                )
                FROM Comment c
                WHERE c.author.userId = :uid
                  AND c.deleted = false
                  AND c.post.deleted = false
                """.trimIndent()

        if (query != null && query.isNotBlank()) {
            base += " AND LOWER(CAST(c.content AS string)) LIKE LOWER(CONCAT('%', :q, '%'))"
        }

        val q = em.createQuery("$base ORDER BY c.createdAt DESC", MyCommentListItem::class.java)
            .setParameter("uid", userId)

        if (query != null && query.isNotBlank()) {
            q.setParameter("q", query.trim())
        }

        val list: List<MyCommentListItem> = q
            .setFirstResult(pageable.offset.toInt())
            .setMaxResults(pageable.pageSize)
            .resultList

        val total: Long = em.createQuery(
            """
                        SELECT COUNT(c.id)
                        FROM Comment c
                        WHERE c.author.userId = :uid AND c.deleted = false
            """.trimIndent(),
            java.lang.Long::class.java
        )
            .setParameter("uid", userId)
            .singleResult
            .toLong()

        return PageImpl(list, pageable, total)
    }
}
