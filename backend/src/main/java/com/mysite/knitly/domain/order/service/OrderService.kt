package com.mysite.knitly.domain.order.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.order.entity.OrderItem
import com.mysite.knitly.domain.order.event.OrderCreatedEvent
import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.domain.payment.entity.Payment
import com.mysite.knitly.domain.payment.entity.PaymentMethod
import com.mysite.knitly.domain.payment.entity.PaymentStatus
import com.mysite.knitly.domain.payment.repository.PaymentRepository
import com.mysite.knitly.domain.product.product.repository.ProductRepository
import com.mysite.knitly.domain.user.entity.User
import com.mysite.knitly.global.email.repository.EmailOutboxRepository
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
    private val eventPublisher: ApplicationEventPublisher,

    private val emailOutboxRepository: EmailOutboxRepository,
    private val objectMapper: ObjectMapper
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

            // 2. 각 Product에 대해 OrderItem을 생성하고, 재고를 직접 감소시킴
            val orderItems = products.map { product ->
                log.trace("[Order] [Service] 재고 감소 - productId={}", product.productId)
                product.decreaseStock(1)
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

            if (paymentStatus == PaymentStatus.DONE) {
                try {
                    log.info("[Order] [Service] 무료 주문 완료 - 이메일 발송 요청(Outbox) 생성 시작")

                    val emailDto = com.mysite.knitly.domain.order.dto.EmailNotificationDto(
                        orderId = savedOrder.orderId ?: 0L,
                        userId = user.userId!!,
                        userEmail = user.email!!
                    )

                    // 객체를 JSON 문자열로 변환
                    val payload = objectMapper.writeValueAsString(emailDto)

                    // Outbox 저장
                    val emailJob = com.mysite.knitly.global.email.entity.EmailOutbox.create(payload)
                    emailOutboxRepository.save(emailJob)

                    log.info("[Order] [Service] 무료 주문 이메일 Outbox 저장 완료")
                } catch (e: Exception) {
                    // 이메일 실패가 주문 전체 실패로 이어지지 않도록 로그만 남김 (선택 사항)
                    log.error("[Order] [Service] 무료 주문 이메일 Outbox 저장 실패", e)
                    // 만약 이메일이 필수라면 여기서 throw e를 해서 주문을 롤백시켜야 함
                }
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