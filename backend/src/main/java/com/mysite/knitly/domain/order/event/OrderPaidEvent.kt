package com.mysite.knitly.domain.order.event

/**
 * 주문 결제가 완료되어 후속 알림(이메일 등)이 필요함을 알리는 이벤트.
 *
 * - OrderService: 무료 주문(0원) 생성 시 바로 발행
 * - PaymentService: 유료 주문의 결제 승인 성공 시 발행
 *
 * 리스너는 @TransactionalEventListener(AFTER_COMMIT) 로 구독하여,
 * 원본 트랜잭션이 완전히 커밋된 후에만 실행되어야 한다.
 */
data class OrderPaidEvent(
    val orderId: Long,
    val userEmail: String
)
