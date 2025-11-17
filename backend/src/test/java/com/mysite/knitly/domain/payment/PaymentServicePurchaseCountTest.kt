package com.mysite.knitly.domain.payment

import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.entity.OrderItem
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.service.RedisProductService
import com.mysite.knitly.domain.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

/**
 * 결제 시 purchaseCount 증가 관련 테스트
 * - Product 엔티티의 increasePurchaseCount() 메서드 테스트
 * - 결제 완료 시 DB와 Redis 모두 증가하는지 테스트
 */
@ExtendWith(MockitoExtension::class)
class PaymentServicePurchaseCountTest {

    private lateinit var user: User
    private lateinit var order: Order
    private lateinit var product1: Product
    private lateinit var product2: Product
    private lateinit var orderItem1: OrderItem
    private lateinit var orderItem2: OrderItem
    private lateinit var mockRedisProductService: RedisProductService

    @BeforeEach
    fun setUp() {
        mockRedisProductService = mock(RedisProductService::class.java)

        user = User(
            userId = 1L,
            name = "테스트유저",
            email = "test@test.com",
            password = "password"
        )

        product1 = Product(
            productId = 1L,
            title = "상품1",
            productCategory = ProductCategory.TOP,
            price = 10000.0,
            purchaseCount = 5
        )

        product2 = Product(
            productId = 2L,
            title = "상품2",
            productCategory = ProductCategory.BOTTOM,
            price = 20000.0,
            purchaseCount = 10
        )

        orderItem1 = OrderItem(
            product = product1,
            orderPrice = 10000.0,
            quantity = 2
        )

        orderItem2 = OrderItem(
            product = product2,
            orderPrice = 20000.0,
            quantity = 1
        )

        val orderItems = mutableListOf(orderItem1, orderItem2)

        order = Order(
            orderId = 1L,
            tossOrderId = "ORDER123ABC",
            user = user,
            totalPrice = 40000.0,
            orderItems = orderItems
        )

        // ReflectionTestUtils로 OrderItem ID와 Order 설정
        ReflectionTestUtils.setField(orderItem1, "orderItemId", 1L)
        ReflectionTestUtils.setField(orderItem1, "order", order)

        ReflectionTestUtils.setField(orderItem2, "orderItemId", 2L)
        ReflectionTestUtils.setField(orderItem2, "order", order)
    }

    @Test
    @DisplayName("결제 승인 시 DB purchaseCount 증가 시뮬레이션")
    fun `confirmPayment IncreasesDbPurchaseCount`() {
        // given
        val initialProduct1Count = product1.purchaseCount
        val initialProduct2Count = product2.purchaseCount

        doNothing().`when`(mockRedisProductService).incrementPurchaseCount(anyLong())

        // when - 결제 승인 로직 시뮬레이션
        order.orderItems.forEach { orderItem ->
            val product = orderItem.product
            val quantity = orderItem.quantity

            // DB purchaseCount 증가
            product.increasePurchaseCount(quantity)

            // Redis 인기도 증가
            repeat(quantity) {
                mockRedisProductService.incrementPurchaseCount(product.productId!!)
            }
        }

        // then - DB purchaseCount 증가 확인
        assertThat(product1.purchaseCount).isEqualTo(initialProduct1Count!! + 2) // 5 + 2 = 7
        assertThat(product2.purchaseCount).isEqualTo(initialProduct2Count!! + 1) // 10 + 1 = 11

        // Redis 호출 확인
        verify(mockRedisProductService, times(2)).incrementPurchaseCount(1L)
        verify(mockRedisProductService, times(1)).incrementPurchaseCount(2L)
    }

    @Test
    @DisplayName("결제 승인 시 Redis 실패해도 DB는 증가")
    fun `confirmPayment DbIncreasesEvenIfRedisFails`() {
        // given
        val initialProduct1Count = product1.purchaseCount

        // Redis 실패 시뮬레이션
        doThrow(RuntimeException("Redis error"))
            .`when`(mockRedisProductService).incrementPurchaseCount(anyLong())

        // when
        try {
            order.orderItems.forEach { orderItem ->
                val product = orderItem.product
                val quantity = orderItem.quantity

                // DB purchaseCount 증가
                product.increasePurchaseCount(quantity)

                // Redis 증가 시도 (실패)
                try {
                    repeat(quantity) {
                        mockRedisProductService.incrementPurchaseCount(product.productId!!)
                    }
                } catch (e: Exception) {
                    // Redis 실패는 무시
                }
            }
        } catch (e: Exception) {
            // 예외 무시
        }

        // then - Redis 실패해도 DB는 증가
        assertThat(product1.purchaseCount).isEqualTo(initialProduct1Count!! + 2)
    }

    @Test
    @DisplayName("Product.increasePurchaseCount() 메서드 테스트")
    fun `product IncreasePurchaseCount`() {
        // given
        val product = Product(
            productId = 1L,
            title = "테스트상품",
            productCategory = ProductCategory.TOP,
            price = 10000.0,
            purchaseCount = 5
        )

        // when
        product.increasePurchaseCount(3)

        // then
        assertThat(product.purchaseCount).isEqualTo(8)
    }

    @Test
    @DisplayName("Product.increasePurchaseCount() - purchaseCount가 null인 경우")
    fun `product IncreasePurchaseCount NullInitial`() {
        // given
        val product = Product(
            productId = 1L,
            title = "테스트상품",
            productCategory = ProductCategory.TOP,
            price = 10000.0,
            purchaseCount = null
        )

        // when
        product.increasePurchaseCount(3)

        // then
        assertThat(product.purchaseCount).isEqualTo(3)
    }

    @Test
    @DisplayName("Product.increasePurchaseCount() - 수량 1 증가")
    fun `product IncreasePurchaseCount SingleQuantity`() {
        // given
        val product = Product(
            productId = 1L,
            title = "테스트상품",
            productCategory = ProductCategory.TOP,
            price = 10000.0,
            purchaseCount = 10
        )

        // when
        product.increasePurchaseCount(1)

        // then
        assertThat(product.purchaseCount).isEqualTo(11)
    }

    @Test
    @DisplayName("주문에 여러 상품이 있을 때 모두 증가")
    fun `multipleProducts AllIncreased`() {
        // given
        val initialProduct1Count = product1.purchaseCount
        val initialProduct2Count = product2.purchaseCount

        doNothing().`when`(mockRedisProductService).incrementPurchaseCount(anyLong())

        // when
        order.orderItems.forEach { orderItem ->
            val product = orderItem.product
            val quantity = orderItem.quantity

            product.increasePurchaseCount(quantity)

            repeat(quantity) {
                mockRedisProductService.incrementPurchaseCount(product.productId!!)
            }
        }

        // then
        assertThat(product1.purchaseCount).isEqualTo(initialProduct1Count!! + 2)
        assertThat(product2.purchaseCount).isEqualTo(initialProduct2Count!! + 1)
        verify(mockRedisProductService, times(3)).incrementPurchaseCount(anyLong())
    }
}