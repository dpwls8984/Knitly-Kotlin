package com.mysite.knitly.domain.order.repository;

import com.mysite.knitly.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // tossOrderId로 주문 조회 (결제 승인 시 사용)
    Optional<Order> findByTossOrderId(String tossOrderId);
}