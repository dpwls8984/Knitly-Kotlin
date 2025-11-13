package com.mysite.knitly.domain.order.service;

import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.entity.OrderItem;
import com.mysite.knitly.domain.order.event.OrderCreatedEvent;
import com.mysite.knitly.domain.order.repository.OrderRepository;
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
    private final ApplicationEventPublisher eventPublisher;

    // Facade에서만 호출될 핵심 비즈니스 로직
    @Transactional
    public Order createOrder(User user, List<Long> productIds) {
    // 1. 요청된 상품 ID 리스트로 모든 Product 엔티티를 조회
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new EntityNotFoundException("일부 상품을 찾을 수 없습니다.");
        }

        // 2. 각 Product에 대해 OrderItem을 빌더로 생성하고, 재고를 직접 감소시킴
        List<OrderItem> orderItems = products.stream()
                .map(product -> {
                    product.decreaseStock(1);
                    return OrderItem.builder()
                            .product(product)
                            .orderPrice(product.getPrice())
                            .quantity(1)
                            .build();
                })
                .collect(Collectors.toList());

        // 3. Order 엔티티 생성
        Order order = Order.create(user, orderItems);

        Order savedOrder = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderCreatedEvent(products));

        return savedOrder;
    }
}