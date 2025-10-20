/*
package com.mysite.knitly.domain.order.service;

import com.mysite.knitly.domain.order.dto.OrderCreateRequest;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.repository.ProductRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.domain.user.repository.UserRepositoryTmp;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepositoryTmp userRepository;

    @Test
    @DisplayName("성공: 한정/상시 판매 상품을 함께 주문하면 정상적으로 주문이 생성된다")
    void createOrder_Success() {
        UUID userId = UUID.randomUUID();
        User fakeUser = User.builder().userId(userId).build();

        Product limitedProduct = Product.builder().productId(1L).price(10000.0).stockQuantity(5).isDeleted(false).build();
        Product unlimitedProduct = Product.builder().productId(2L).price(15000.0).stockQuantity(null).isDeleted(false).build();

        OrderCreateRequest request = new OrderCreateRequest(List.of(1L, 2L));

        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(limitedProduct));
        when(productRepository.findById(2L)).thenReturn(Optional.of(unlimitedProduct));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order orderToSave = invocation.getArgument(0);

            return orderToSave;
        });

        // Mock Product 객체에 대해 spy를 사용하여 실제 메서드(decreaseStock) 호출을 추적
        Product spyLimitedProduct = spy(limitedProduct);
        when(productRepository.findById(1L)).thenReturn(Optional.of(spyLimitedProduct));

        var response = orderService.createOrder(userId, request);

        // 1. 응답 데이터 검증
        assertThat(response.totalPrice()).isEqualTo(25000.0);
        assertThat(response.orderItems()).hasSize(2);

        // 2. 재고 차감 로직 검증
        verify(spyLimitedProduct, times(1)).decreaseStock(1); // 한정 상품의 재고만 1번 차감되었는지 확인

        // 3. 주문 저장 데이터 검증
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getTotalPrice()).isEqualTo(25000.0);
        assertThat(savedOrder.getOrderItems().get(0).getOrderPrice()).isEqualTo(10000.0);
    }

    @Test
    @DisplayName("실패: 품절된 상품을 주문하면 예외가 발생한다")
    void createOrder_Fail_OutOfStock() {
        UUID userId = UUID.randomUUID();
        User fakeUser = User.builder().userId(userId).build();
        Product soldOutProduct = Product.builder().productId(1L).price(10000.0).stockQuantity(0).isDeleted(false).build();
        OrderCreateRequest request = new OrderCreateRequest(List.of(1L));

        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(soldOutProduct));

        Product spySoldOutProduct = spy(soldOutProduct);
        when(productRepository.findById(1L)).thenReturn(Optional.of(spySoldOutProduct));

        ServiceException exception = assertThrows(ServiceException.class, () -> {
            orderService.createOrder(userId, request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("실패: 판매 중지된 상품을 주문하면 예외가 발생한다")
    void createOrder_Fail_ProductIsDeleted() {
        UUID userId = UUID.randomUUID();
        User fakeUser = User.builder().userId(userId).build();
        Product deletedProduct = Product.builder().productId(1L).isDeleted(true).build();
        OrderCreateRequest request = new OrderCreateRequest(List.of(1L));

        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));
        when(productRepository.findById(1L)).thenReturn(Optional.of(deletedProduct));

        ServiceException exception = assertThrows(ServiceException.class, () -> {
            orderService.createOrder(userId, request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_ALREADY_DELETED);
    }

    @Test
    @DisplayName("실패: 남은 재고보다 많은 수량을 주문하면 예외가 발생한다")
    void createOrder_Fail_StockInsufficient() {
        UUID userId = UUID.randomUUID();
        User fakeUser = User.builder().userId(userId).build();

        // 재고가 1개만 남은 상품을 준비
        Product limitedProduct = Product.builder()
                .productId(1L)
                .price(10000.0)
                .stockQuantity(1) // ⚠️ 재고 1개
                .isDeleted(false)
                .build();

        // 재고가 1개인 상품을 2개 주문하는 요청을 생성
        OrderCreateRequest request = new OrderCreateRequest(List.of(1L, 1L)); // ⚠️ 2개 주문

        when(userRepository.findById(userId)).thenReturn(Optional.of(fakeUser));

        Product spyLimitedProduct = spy(limitedProduct);
        when(productRepository.findById(1L)).thenReturn(Optional.of(spyLimitedProduct));

        // 주문 생성 시 Product.decreaseStock() 내부에서 예외가 발생하는지 검증
        ServiceException exception = assertThrows(ServiceException.class, () -> {
            orderService.createOrder(userId, request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_STOCK_INSUFFICIENT);

        // 예외가 발생했으므로, save는 절대 호출되지 않았어야 함
        verify(orderRepository, never()).save(any(Order.class));
    }
}*/
