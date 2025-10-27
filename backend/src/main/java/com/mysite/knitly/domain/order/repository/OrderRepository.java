package com.mysite.knitly.domain.order.repository;

import com.mysite.knitly.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ✅ 추가: 이메일 발송 시 사용할 상세 정보 조회 쿼리
    @Query("SELECT o FROM Order o " +
            "JOIN FETCH o.user u " +
            "JOIN FETCH o.orderItems oi " +
            "JOIN FETCH oi.product p " +
            "JOIN FETCH p.design d " +
            "WHERE o.orderId = :orderId")
    Optional<Order> findOrderWithDetailsById(@Param("orderId") Long orderId);

    // tossOrderId로 주문 조회 (결제 승인 시 사용)
    Optional<Order> findByTossOrderId(String tossOrderId);
}