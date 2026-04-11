package com.mysite.knitly.domain.order.repository

import com.mysite.knitly.domain.order.entity.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderRepository : JpaRepository<Order, Long> {
    // tossOrderId로 주문 조회 (결제 승인 시 사용)
    fun findByTossOrderId(tossOrderId: String): Order?

    /**
     * 이메일 발송에 필요한 모든 연관 엔티티를 한 번의 쿼리로 로드한다.
     *
     * - orderItems (OneToMany, LAZY)
     * - orderItems.product (ManyToOne, LAZY)
     * - orderItems.product.design (ManyToOne, LAZY) → PDF 첨부에 필요
     * - user (ManyToOne, LAZY) → 이메일 수신자/이름
     *
     * @Async 이메일 리스너가 새 트랜잭션에서 호출하여, 이후 Session이 닫혀도
     * order 내부 필드 접근 시 LazyInitializationException이 발생하지 않도록 한다.
     *
     * DISTINCT는 orderItems 조인으로 인한 Order row 중복을 제거하기 위해 필요.
     */
    @Query(
        """
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.product p
        LEFT JOIN FETCH p.design
        LEFT JOIN FETCH o.user
        WHERE o.orderId = :orderId
        """
    )
    fun findByIdWithItems(@Param("orderId") orderId: Long): Order?
}