package com.mysite.knitly.domain.order.event

import com.mysite.knitly.domain.order.repository.OrderRepository
import com.mysite.knitly.global.email.service.EmailService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * OrderPaidEvent를 수신하여 주문 확인 이메일을 비동기로 발송하는 리스너.
 *
 * 설계 포인트:
 *  1) @TransactionalEventListener(AFTER_COMMIT)
 *     - 발행자(OrderService/PaymentService)의 트랜잭션이 커밋 성공한 이후에만 실행
 *     - "커밋 전에 조회" race condition 원천 차단
 *
 *  2) @Async("emailExecutor")
 *     - 리스너 자체를 비동기로 실행 → HTTP 응답 지연 없음
 *     - graceful shutdown 적용된 전용 스레드풀(emailExecutor) 사용
 *
 *  3) @Transactional(propagation = REQUIRES_NEW)
 *     - 원본 트랜잭션은 이미 종료됨. 이 메서드는 완전히 독립된 새 트랜잭션에서 실행
 *     - findByIdWithItems 의 fetch join 결과를 로드할 세션 확보 목적
 *
 *  4) EmailService 호출 시 이미 fetch join으로 모든 LAZY 필드가 초기화된 Order를 전달
 *     → EmailService 내부에서 LazyInitializationException 발생 불가
 */
@Component
class OrderEmailNotificationListener(
    private val orderRepository: OrderRepository,
    private val emailService: EmailService
) {
    companion object {
        private val log = LoggerFactory.getLogger(OrderEmailNotificationListener::class.java)
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleOrderPaid(event: OrderPaidEvent) {
        log.info("[OrderEmail] [Listener] OrderPaidEvent 수신 - orderId={}", event.orderId)

        val order = orderRepository.findByIdWithItems(event.orderId)
        if (order == null) {
            log.error(
                "[OrderEmail] [Listener] Order를 찾지 못해 이메일 발송 중단 - orderId={}",
                event.orderId
            )
            return
        }

        try {
            emailService.sendOrderConfirmationEmail(order, event.userEmail)
            log.info(
                "[OrderEmail] [Listener] 이메일 발송 요청 완료 - orderId={}, email={}",
                event.orderId, event.userEmail
            )
        } catch (e: Exception) {
            // @Retryable + @Recover 에서 최종 처리됨. 여기까지 올라오는 경우는 극히 드묾.
            log.error(
                "[OrderEmail] [Listener] 이메일 발송 중 예외 발생 - orderId={}, email={}",
                event.orderId, event.userEmail, e
            )
        }
    }
}
