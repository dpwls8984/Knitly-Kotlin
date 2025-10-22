package com.mysite.knitly.domain.order.service;

import com.mysite.knitly.domain.order.dto.EmailNotificationDto;
import com.mysite.knitly.domain.order.dto.OrderCreateRequest;
import com.mysite.knitly.domain.order.dto.OrderCreateResponse;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.entity.OrderItem;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;

    public OrderCreateResponse createOrder(User user, OrderCreateRequest request) {
        // 1. 정렬된 순서로 락 획득 (교착 방지)
        List<Long> sortedIds = request.productIds().stream()
                .sorted()
                .collect(Collectors.toList());

        List<RLock> locks = sortedIds.stream()
                .map(productId -> redissonClient.getLock("lock:product:" + productId))
                .collect(Collectors.toList());

        boolean locked = false;

        try {
            // 2. 모든 락을 미리 획득
            for (RLock lock : locks) {
                if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                    throw new ServiceException(ErrorCode.LOCK_ACQUISITION_FAILED);
                }
            }
            locked = true;

            // 3. 트랜잭션 내부에서 안전하게 주문 생성
            OrderCreateResponse response = createOrderTransactional(user, sortedIds);

            return response;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 대기 중 인터럽트 발생", e);
        } finally {
            // 4. 락은 반드시 역순으로 해제 (획득 순서의 반대)
            if (locked) {
                Collections.reverse(locks);
                for (RLock lock : locks) {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
        }
    }

    @Transactional
    protected OrderCreateResponse createOrderTransactional(User user, List<Long> productIds) {
        // 1. 상품 조회 및 재고 차감
        List<Product> products = productIds.stream()
                .map(id -> {
                    Product product = productRepository.findById(id)
                            .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));
                    if (product.getIsDeleted()) {
                        throw new ServiceException(ErrorCode.PRODUCT_ALREADY_DELETED);
                    }
                    product.decreaseStock(1);
                    return product;
                })
                .collect(Collectors.toList());

        // 2. 주문 생성
        double totalPrice = products.stream().mapToDouble(Product::getPrice).sum();

        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .build();

        for (Product product : products) {
            order.addOrderItem(OrderItem.builder()
                    .product(product)
                    .orderPrice(product.getPrice())
                    .quantity(1)
                    .build());
        }

        Order savedOrder = orderRepository.save(order);

        // 3. 트랜잭션 커밋 이후 메시지 발행 예약
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        EmailNotificationDto emailDto = new EmailNotificationDto(
                                savedOrder.getOrderId(),
                                user.getUserId(),
                                user.getEmail()
                        );
                        rabbitTemplate.convertAndSend("order.exchange", "order.completed", emailDto);
                    }
                });

        return OrderCreateResponse.from(savedOrder);
    }
}
