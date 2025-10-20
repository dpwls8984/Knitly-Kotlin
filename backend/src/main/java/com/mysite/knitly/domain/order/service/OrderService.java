package com.mysite.knitly.domain.order.service;

import com.mysite.knitly.domain.community.post.repository.UserRepository;
import com.mysite.knitly.domain.order.dto.OrderCreateRequest;
import com.mysite.knitly.domain.order.dto.OrderCreateResponse;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.entity.OrderItem;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

        // 2. 주문 상품 조회 및 재고 검증
        List<Product> products = request.productIds().stream()
                .map(productId -> {
                    //TODO: redis 락 획득 로직 필요
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND));

                    // 재고 수량 검증 (한정 상품)
                    if(product.getStockQuantity() != null && product.getStockQuantity() <= 0) {
                        throw new ServiceException(ErrorCode.OUT_OF_STOCK);
                    }

                    // 이미 삭제(판매 중지)된 상품인지 검증
                    if(product.getIsDeleted()) {
                        throw new ServiceException(ErrorCode.PRODUCT_ALREADY_DELETED);
                    }

                    //TODO: redis 락 해제 로직 필요
                    return product;
                }).collect(Collectors.toList());

        // 3. 주문 총액 계산
        double totalPrice = products.stream()
                .mapToDouble(Product::getPrice)
                .sum();

        // 4. Order 엔티티 생성
        Order order = Order.builder()
                .user(user)
                .totalPrice(totalPrice)
                .build();

        // 5. OrderItem 엔티티 생성 및 Order에 추가
        for (Product product : products) {
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .orderPrice(product.getPrice()) // 주문 당시 가격 저장 -> 추후에 가격이 수정됐을 때 영향 받지 않도록
                    .quantity(1)
                    .build();
            order.addOrderItem(orderItem);

            // 6. 재고 수량 차감 (한정 상품)
            if(product.getStockQuantity() != null) {
                //만약 재고 설정을 해놨다면 재고 차감
                product.decreaseStock(1); // TODO: 재고 차감 메서드 구현 필요
                //product 엔티티 안에서 재고 수량 감소 시키는 게 맞나? 왜?
                //-> 재고 수량 관리는 product가 책임지는 게 맞음. 주문 서비스에서 재고를 직접 조작하면 응집도가 떨어짐
            }
        }

        // 7. 주문 저장 (CascadeType.ALL로 OrderItem도 함께 저장됨)
        Order savedOrder = orderRepository.save(order);

        // 8. TODO: 비동기 처리 - 큐에 이메일 발송 메시지 발행

        return OrderCreateResponse.from(savedOrder);

    }
}
