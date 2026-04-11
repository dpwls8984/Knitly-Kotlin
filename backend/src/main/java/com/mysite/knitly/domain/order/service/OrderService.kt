package com.mysite.knitly.domain.order.service

import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.entity.OrderItem
import com.mysite.knitly.domain.order.event.OrderCreatedEvent
import com.mysite.knitly.domain.order.event.OrderPaidEvent
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.exception.ErrorCode
import com.mysite.knitly.global.exception.ServiceException
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OrderService(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    companion object {
        private val log = LoggerFactory.getLogger(OrderService::class.java)
    }

    // Facade에서만 호출될 핵심 비즈니스 로직
    @Transactional
    fun createOrder(user: User, productIds: List<Long>): Order {
        log.info("[Order] [Service] 주문 생성 시작 - userId={}, productIds={}", user.userId, productIds)
        try {
            // 1. 요청된 상품 ID 리스트로 모든 Product 엔티티를 조회
            val products = productRepository.findAllById(productIds)
            log.debug(
                "[Order] [Service] DB 상품 조회 완료 - idCount={}, foundCount={}",
                productIds.size, products.size
            )

            if (products.size != productIds.size) {
                log.warn("[Order] [Service] 상품 조회 불일치 - 요청={}, 찾음={}", productIds.size, products.size)
                throw EntityNotFoundException("일부 상품을 찾을 수 없습니다.")
            }

            // 2. 각 Product에 대해 재고를 원자적으로 차감하고 OrderItem을 생성
            val orderItems = products.map { product ->
                // 상시 판매 상품(stockQuantity == null)은 재고 차감 불필요
                if (product.stockQuantity != null) {
                    val updatedRows = productRepository.decreaseStock(product.productId!!, 1)
                    if (updatedRows == 0) {
                        log.warn("[Order] [Service] 재고 부족 - productId={}", product.productId)
                        throw ServiceException(ErrorCode.PRODUCT_STOCK_INSUFFICIENT)
                    }
                    log.trace("[Order] [Service] 원자적 재고 감소 완료 - productId={}", product.productId)
                }
                OrderItem(
                    product = product,
                    orderPrice = product.price,
                    quantity = 1
                )
            }
            log.debug("[Order] [Service] 재고 감소 및 OrderItems 생성 완료")

            // 3. Order 엔티티 생성
            val order = Order.create(user, orderItems)
            log.debug("[Order] [Service] Order 엔티티 생성 완료")

            // 4. Order 저장 (OrderItem은 CascadeType.ALL에 의해 함께 저장됨)
            val savedOrder = orderRepository.save(order)
            log.debug("[Order] [Service] DB 주문 저장 완료")

            //5. Payment를 READY 상태로 생성 - Order 생성 이후에 실행되어야 함
            val totalPrice = orderItems.sumOf { it.orderPrice }
            val paymentStatus = if (totalPrice == 0.0) PaymentStatus.DONE else PaymentStatus.READY  // 0원이면 바로 DONE
            val approvedAt = if (totalPrice == 0.0) LocalDateTime.now() else null  // 0원이면 승인시간도 설정

            val readyPayment = Payment(
                tossOrderId = savedOrder.tossOrderId,
                order = savedOrder,
                buyer = user,
                totalAmount = savedOrder.totalPrice.toLong(),
                paymentMethod = if (totalPrice == 0.0) PaymentMethod.FREE else PaymentMethod.CARD, // 0원이면 무료 결제
                paymentStatus = paymentStatus,  // 0원이면 DONE, 아니면 READY
                approvedAt = approvedAt  // 0원이면 현재 시간, 아니면 null
            )

            paymentRepository.save(readyPayment)

            if (paymentStatus == PaymentStatus.DONE && user.email != null) {
                // 이메일 발송은 OrderEmailNotificationListener가 AFTER_COMMIT 시점에 비동기로 처리.
                // 여기서는 이벤트만 발행한다.
                eventPublisher.publishEvent(
                    OrderPaidEvent(
                        orderId = savedOrder.orderId!!,
                        userEmail = user.email!!
                    )
                )
                log.info("[Order] [Service] 무료 주문 OrderPaidEvent 발행 완료 - orderId={}", savedOrder.orderId)
            }

            eventPublisher.publishEvent(OrderCreatedEvent(products))
            log.debug("[Order] [Service] OrderCreatedEvent 발행 완료")

            log.info("[Order] [Service] 주문 생성 성공 - orderId={}", savedOrder.orderId)

            return savedOrder
        } catch (e: Exception) {
            log.error(
                "[Order] [Service] 주문 생성 실패 - userId={}, productIds={}",
                user.userId, productIds, e
            )
            throw e
        }
    }
}