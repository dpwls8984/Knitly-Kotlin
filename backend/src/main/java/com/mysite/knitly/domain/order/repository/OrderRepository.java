package com.mysite.knitly.domain.order.repository;

import com.mysite.knitly.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
