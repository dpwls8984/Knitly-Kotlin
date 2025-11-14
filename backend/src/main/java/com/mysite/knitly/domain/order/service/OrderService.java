package com.mysite.knitly.domain.order.service;

import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.entity.OrderItem;
import com.mysite.knitly.domain.order.event.OrderCreatedEvent;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentMethod;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import com.mysite.knitly.domain.payment.repository.PaymentRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Facade에서만 호출될 핵심 비즈니스 로직
    @Transactional
    public Order createOrder(User user, List<Long> productIds) {
        log.info("[Order] [Service] 주문 생성 시작 - userId={}, productIds={}", user.getUserId(), productIds);
        try {
            // 1. 요청된 상품 ID 리스트로 모든 Product 엔티티를 조회
            List<Product> products = productRepository.findAllById(productIds);
            log.debug("[Order] [Service] DB 상품 조회 완료 - idCount={}, foundCount={}",
                    productIds.size(), products.size());

            if (products.size() != productIds.size()) {
                log.warn("[Order] [Service] 상품 조회 불일치 - 요청={}, 찾음={}", productIds.size(), products.size());
                throw new EntityNotFoundException("일부 상품을 찾을 수 없습니다.");
            }

            // 2. 각 Product에 대해 OrderItem을 빌더로 생성하고, 재고를 직접 감소시킴
            List<OrderItem> orderItems = products.stream()
                    .map(product -> {
                        log.trace("[Order] [Service] 재고 감소 - productId={}", product.getProductId());
                        product.decreaseStock(1);
                        return OrderItem.builder()
                                .product(product)
                                .orderPrice(product.getPrice())
                                .quantity(1)
                                .build();
                    })
                    .collect(Collectors.toList());
            log.debug("[Order] [Service] 재고 감소 및 OrderItems 생성 완료");

            // 3. Order 엔티티 생성
            Order order = Order.create(user, orderItems);
            log.debug("[Order] [Service] Order 엔티티 생성 완료");

            // 4. Order 저장 (OrderItem은 CascadeType.ALL에 의해 함께 저장됨)
            Order savedOrder = orderRepository.save(order);
            log.debug("[Order] [Service] DB 주문 저장 완료");

            //5. Payment를 READY 상태로 생성 - Order 생성 이후에 실행되어야 함
            Payment readyPayment = Payment.builder()
                    .tossOrderId(savedOrder.getTossOrderId())
                    .order(savedOrder)
                    .buyer(user)
                    .totalAmount(savedOrder.getTotalPrice().longValue())
                    .paymentMethod(PaymentMethod.CARD)  // 초기값 (나중에 실제 결제 수단으로 업데이트됨)
                    .paymentStatus(PaymentStatus.READY)  // READY 상태
                    .build();

            paymentRepository.save(readyPayment);

            eventPublisher.publishEvent(new OrderCreatedEvent(products));
            log.debug("[Order] [Service] OrderCreatedEvent 발행 완료");

            log.info("[Order] [Service] 주문 생성 성공 - orderId={}", savedOrder.getOrderId());

            return savedOrder;
        } catch (Exception e) {
            log.error("[Order] [Service] 주문 생성 실패 - userId={}, productIds={}",
                    user.getUserId(), productIds, e);
            throw e;
        }
    }
}