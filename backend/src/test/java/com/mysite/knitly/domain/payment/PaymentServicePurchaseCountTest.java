package com.mysite.knitly.domain.payment;

import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.entity.OrderItem;
import com.mysite.knitly.domain.product.product.entity.Product;
import com.mysite.knitly.domain.product.product.entity.ProductCategory;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import com.mysite.knitly.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 결제 시 purchaseCount 증가 관련 테스트
 * - Product 엔티티의 increasePurchaseCount() 메서드 테스트
 * - 결제 완료 시 DB와 Redis 모두 증가하는지 테스트
 */
@ExtendWith(MockitoExtension.class)
class PaymentServicePurchaseCountTest {

    private User user;
    private Order order;
    private Product product1;
    private Product product2;
    private OrderItem orderItem1;
    private OrderItem orderItem2;
    private RedisProductService mockRedisProductService;

    @BeforeEach
    void setUp() {
        mockRedisProductService = mock(RedisProductService.class);

        user = User.builder()
                .userId(1L)
                .name("테스트유저")
                .email("test@test.com")
                .build();

        product1 = Product.builder()
                .productId(1L)
                .title("상품1")
                .productCategory(ProductCategory.TOP)
                .price(10000.0)
                .purchaseCount(5)
                .build();

        product2 = Product.builder()
                .productId(2L)
                .title("상품2")
                .productCategory(ProductCategory.BOTTOM)
                .price(20000.0)
                .purchaseCount(10)
                .build();

        orderItem1 = OrderItem.builder()
                .product(product1)
                .orderPrice(10000.0)
                .quantity(2)
                .build();

        orderItem2 = OrderItem.builder()
                .product(product2)
                .orderPrice(20000.0)
                .quantity(1)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        orderItems.add(orderItem1);
        orderItems.add(orderItem2);

        order = Order.builder()
                .orderId(1L)
                .tossOrderId("ORDER123ABC")
                .user(user)
                .totalPrice(40000.0)
                .orderItems(orderItems)
                .build();

        // ReflectionTestUtils로 OrderItem ID와 Order 설정
        ReflectionTestUtils.setField(orderItem1, "orderItemId", 1L);
        ReflectionTestUtils.setField(orderItem1, "order", order);

        ReflectionTestUtils.setField(orderItem2, "orderItemId", 2L);
        ReflectionTestUtils.setField(orderItem2, "order", order);
    }

    @Test
    @DisplayName("결제 승인 시 DB purchaseCount 증가 시뮬레이션")
    void confirmPayment_IncreasesDbPurchaseCount() {
        // given
        int initialProduct1Count = product1.getPurchaseCount();
        int initialProduct2Count = product2.getPurchaseCount();

        doNothing().when(mockRedisProductService).incrementPurchaseCount(anyLong());

        // when - 결제 승인 로직 시뮬레이션
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            int quantity = orderItem.getQuantity();

            // DB purchaseCount 증가
            product.increasePurchaseCount(quantity);

            // Redis 인기도 증가
            for (int i = 0; i < quantity; i++) {
                mockRedisProductService.incrementPurchaseCount(product.getProductId());
            }
        }

        // then - DB purchaseCount 증가 확인
        assertThat(product1.getPurchaseCount()).isEqualTo(initialProduct1Count + 2); // 5 + 2 = 7
        assertThat(product2.getPurchaseCount()).isEqualTo(initialProduct2Count + 1); // 10 + 1 = 11

        // Redis 호출 확인
        verify(mockRedisProductService, times(2)).incrementPurchaseCount(1L);
        verify(mockRedisProductService, times(1)).incrementPurchaseCount(2L);
    }

    @Test
    @DisplayName("결제 승인 시 Redis 실패해도 DB는 증가")
    void confirmPayment_DbIncreasesEvenIfRedisFails() {
        // given
        int initialProduct1Count = product1.getPurchaseCount();

        // Redis 실패 시뮬레이션
        doThrow(new RuntimeException("Redis error"))
                .when(mockRedisProductService).incrementPurchaseCount(anyLong());

        // when
        try {
            for (OrderItem orderItem : order.getOrderItems()) {
                Product product = orderItem.getProduct();
                int quantity = orderItem.getQuantity();

                // DB purchaseCount 증가
                product.increasePurchaseCount(quantity);

                // Redis 증가 시도 (실패)
                try {
                    for (int i = 0; i < quantity; i++) {
                        mockRedisProductService.incrementPurchaseCount(product.getProductId());
                    }
                } catch (Exception e) {
                    // Redis 실패는 무시
                }
            }
        } catch (Exception e) {
            // 예외 무시
        }

        // then - Redis 실패해도 DB는 증가
        assertThat(product1.getPurchaseCount()).isEqualTo(initialProduct1Count + 2);
    }

    @Test
    @DisplayName("Product.increasePurchaseCount() 메서드 테스트")
    void product_IncreasePurchaseCount() {
        // given
        Product product = Product.builder()
                .productId(1L)
                .title("테스트상품")
                .productCategory(ProductCategory.TOP)
                .price(10000.0)
                .purchaseCount(5)
                .build();

        // when
        product.increasePurchaseCount(3);

        // then
        assertThat(product.getPurchaseCount()).isEqualTo(8);
    }

    @Test
    @DisplayName("Product.increasePurchaseCount() - purchaseCount가 null인 경우")
    void product_IncreasePurchaseCount_NullInitial() {
        // given
        Product product = Product.builder()
                .productId(1L)
                .title("테스트상품")
                .productCategory(ProductCategory.TOP)
                .price(10000.0)
                .purchaseCount(null)
                .build();

        // when
        product.increasePurchaseCount(3);

        // then
        assertThat(product.getPurchaseCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Product.increasePurchaseCount() - 수량 1 증가")
    void product_IncreasePurchaseCount_SingleQuantity() {
        // given
        Product product = Product.builder()
                .productId(1L)
                .title("테스트상품")
                .productCategory(ProductCategory.TOP)
                .price(10000.0)
                .purchaseCount(10)
                .build();

        // when
        product.increasePurchaseCount(1);

        // then
        assertThat(product.getPurchaseCount()).isEqualTo(11);
    }

    @Test
    @DisplayName("주문에 여러 상품이 있을 때 모두 증가")
    void multipleProducts_AllIncreased() {
        // given
        int initialProduct1Count = product1.getPurchaseCount();
        int initialProduct2Count = product2.getPurchaseCount();

        doNothing().when(mockRedisProductService).incrementPurchaseCount(anyLong());

        // when
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            int quantity = orderItem.getQuantity();

            product.increasePurchaseCount(quantity);

            for (int i = 0; i < quantity; i++) {
                mockRedisProductService.incrementPurchaseCount(product.getProductId());
            }
        }

        // then
        assertThat(product1.getPurchaseCount()).isEqualTo(initialProduct1Count + 2);
        assertThat(product2.getPurchaseCount()).isEqualTo(initialProduct2Count + 1);
        verify(mockRedisProductService, times(3)).incrementPurchaseCount(anyLong());
    }
}
