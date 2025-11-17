package com.mysite.knitly.domain.payment.entity

import com.mysite.knitly.domain.order.entity.Order
import com.mysite.knitly.domain.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener::class)
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var paymentId: Long ?= null,

    // READY 상태에서는 tossPaymentKey가 아직 없으므로 null 허용
    @Column(unique = true)
    var tossPaymentKey: String? = null,

    @Column(nullable = false)
    var tossOrderId: String,

    @Column(length = 100)
    var mid: String? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var buyer: User,

    @Column(nullable = false)
    var totalAmount: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentMethod: PaymentMethod,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentStatus: PaymentStatus,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var requestedAt: LocalDateTime? = null,

    @Column
    var approvedAt: LocalDateTime? = null,

    @Column
    var canceledAt: LocalDateTime? = null,

    @Column(length = 500)
    var cancelReason: String? = null,

    @Column(length = 1000)
    var failureReason: String? = null
) {

    /**
     * 결제 승인 처리
     */
    fun approve(approvedAt: LocalDateTime = LocalDateTime.now()) {
        paymentStatus = PaymentStatus.DONE
        this.approvedAt = approvedAt
    }

    /**
     * 결제 취소 처리
     */
    fun cancel(cancelReason: String) {
        check(isCancelable) { "취소할 수 없는 결제 상태입니다: $paymentStatus" }

        paymentStatus = PaymentStatus.CANCELED
        canceledAt = LocalDateTime.now()
        this.cancelReason = cancelReason
    }

    /**
     * 결제 실패 처리
     */
    fun fail(failureReason: String) {
        paymentStatus = PaymentStatus.FAILED
        this.failureReason = failureReason
    }

    val isCompleted: Boolean
        get() = paymentStatus == PaymentStatus.DONE

    val isCancelable: Boolean
        get() = paymentStatus.isCancelable
}
