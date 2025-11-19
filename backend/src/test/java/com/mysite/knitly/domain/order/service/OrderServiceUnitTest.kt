package com.mysite.knitly.domain.order.service

import com.mysite.knitly.domain.design.entity.Design
import com.mysite.knitly.domain.design.entity.DesignState
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.event.OrderCreatedEvent
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.domain.product.product.entity.Product
import com.mysite.knitly.domain.product.product.entity.ProductCategory
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.user.entity.Provider
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher

@ExtendWith(MockitoExtension::class)
class OrderServiceUnitTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var orderRepository: OrderRepository

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @InjectMocks
    private lateinit var orderService: OrderService

    // 테스트 데이터 (Real Objects)
    private lateinit var buyer: User
    private lateinit var seller: User
    private lateinit var design: Design
    private lateinit var limitedProduct: Product  // 한정 판매 (재고 있음)
    private lateinit var unlimitedProduct: Product // 상시 판매 (재고 null)
    private lateinit var freeProduct: Product      // 무료 상품

    @BeforeEach
    fun setUp() {
        buyer = User(
            userId = 1L,
            email = "buyer@test.com",
            name = "구매자",
            socialId = "social_buyer",
            provider = Provider.GOOGLE
        )
        seller = User(
            userId = 2L,
            email = "seller@test.com",
            name = "판매자",
            socialId = "social_seller",
            provider = Provider.GOOGLE
        )

        design = Design(
            designId = 1L,
            user = seller,
            designName = "테스트 도안",
            gridData = "{}",
            designState = DesignState.ON_SALE,
            pdfUrl = "url"
        )

        // 한정 판매 상품 (재고 10개)
        limitedProduct = Product(
            productId = 10L,
            title = "한정판 니트",
            description = "설명",
            productCategory = ProductCategory.TOP,
            sizeInfo = "M",
            price = 10000.0,
            user = seller,
            design = design,
            stockQuantity = 10,
            isDeleted = false
        )

        // 상시 판매 상품 (재고 null)
        unlimitedProduct = Product(
            productId = 20L,
            title = "상시 판매 니트",
            description = "설명",
            productCategory = ProductCategory.TOP,
            sizeInfo = "L",
            price = 15000.0,
            user = seller,
            design = design,
            stockQuantity = null, // 중요: 상시 판매
            isDeleted = false
        )

        // 무료 상품
        freeProduct = Product(
            productId = 30L,
            title = "무료 도안",
            description = "설명",
            productCategory = ProductCategory.ETC,
            sizeInfo = "Free",
            price = 0.0,
            user = seller,
            design = design,
            stockQuantity = null,
            isDeleted = false
        )
    }

    @Test
    @DisplayName("주문 성공(유료/한정판매): 재고 감소 및 결제 READY 상태 검증")
    fun createOrder_Success_LimitedStock() {
        // Given
        val productIds = listOf(10L)
        given(productRepository.findAllById(productIds)).willReturn(listOf(limitedProduct))

        // save 모킹: 들어온 엔티티를 그대로 반환
        given(orderRepository.save(any<Order>())).willAnswer { it.getArgument(0) }

        // When
        val resultOrder = orderService.createOrder(buyer, productIds)

        // Then
        // 1. 재고 감소 확인 (10 -> 9)
        assertThat(limitedProduct.stockQuantity).isEqualTo(9)

        // 2. 주문 객체 검증
        assertThat(resultOrder.totalPrice).isEqualTo(10000.0)
        assertThat(resultOrder.orderItems).hasSize(1)
        assertThat(resultOrder.tossOrderId).isNotNull() // Order.create 내부 로직 동작 확인

        // 3. 결제 저장 검증
        val paymentCaptor = argumentCaptor<Payment>()
        verify(paymentRepository).save(paymentCaptor.capture())
        val savedPayment = paymentCaptor.firstValue

        assertThat(savedPayment.paymentStatus).isEqualTo(PaymentStatus.READY)
        assertThat(savedPayment.totalAmount).isEqualTo(10000L)
        assertThat(savedPayment.approvedAt).isNull()
    }

    @Test
    @DisplayName("주문 성공(유료/상시판매): 재고가 null이면 감소하지 않고 유지됨")
    fun createOrder_Success_UnlimitedStock() {
        // Given
        val productIds = listOf(20L)
        given(productRepository.findAllById(productIds)).willReturn(listOf(unlimitedProduct))
        given(orderRepository.save(any<Order>())).willAnswer { it.getArgument(0) }

        // When
        orderService.createOrder(buyer, productIds)

        // Then
        // 재고가 null이어야 함 (Product.decreaseStock의 로직 검증)
        assertThat(unlimitedProduct.stockQuantity).isNull()

        verify(orderRepository).save(any())
        verify(paymentRepository).save(any())
    }

    @Test
    @DisplayName("주문 성공(무료): 결제가 바로 DONE 상태로 저장됨")
    fun createOrder_Success_Free() {
        // Given
        val productIds = listOf(30L)
        given(productRepository.findAllById(productIds)).willReturn(listOf(freeProduct))
        given(orderRepository.save(any<Order>())).willAnswer { it.getArgument(0) }

        // When
        val resultOrder = orderService.createOrder(buyer, productIds)

        // Then
        val paymentCaptor = argumentCaptor<Payment>()
        verify(paymentRepository).save(paymentCaptor.capture())
        val savedPayment = paymentCaptor.firstValue

        assertThat(savedPayment.paymentStatus).isEqualTo(PaymentStatus.DONE) // 바로 완료
        assertThat(savedPayment.paymentMethod).isEqualTo(PaymentMethod.FREE)
        assertThat(savedPayment.totalAmount).isEqualTo(0L)
        assertThat(savedPayment.approvedAt).isNotNull() // 승인 시간 존재
    }

    @Test
    @DisplayName("주문 실패: 재고 부족 시 ServiceException(PRODUCT_STOCK_INSUFFICIENT) 발생")
    fun createOrder_Fail_OutOfStock() {
        // Given
        // 재고를 0으로 강제 설정 (Product 엔티티 조작)
        limitedProduct.decreaseStock(10) // 10 - 10 = 0
        assertThat(limitedProduct.stockQuantity).isEqualTo(0)

        val productIds = listOf(10L)
        given(productRepository.findAllById(productIds)).willReturn(listOf(limitedProduct))

        // When & Then
        assertThatThrownBy {
            orderService.createOrder(buyer, productIds)
        }
            .isInstanceOf(ServiceException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_STOCK_INSUFFICIENT)

        // 검증: 예외 발생 시 저장 로직이 호출되지 않았는지 확인
        verify(orderRepository, never()).save(any())
        verify(paymentRepository, never()).save(any())
        verify(eventPublisher, never()).publishEvent(any())
    }

    @Test
    @DisplayName("주문 실패: 요청한 상품 ID와 조회된 상품 수가 다르면 예외 발생")
    fun createOrder_Fail_ProductNotFound() {
        // Given
        val productIds = listOf(10L, 999L) // 999L은 DB에 없음
        given(productRepository.findAllById(productIds)).willReturn(listOf(limitedProduct)) // 하나만 반환

        // When & Then
        assertThatThrownBy {
            orderService.createOrder(buyer, productIds)
        }
            .isInstanceOf(EntityNotFoundException::class.java)
            .hasMessage("일부 상품을 찾을 수 없습니다.")
    }
}