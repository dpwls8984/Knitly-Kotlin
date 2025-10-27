package com.mysite.knitly.domain.payment.repository;

import com.mysite.knitly.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder_OrderId(Long orderId);
    Optional<Payment> findByTossPaymentKey(String tossPaymentKey);
}
