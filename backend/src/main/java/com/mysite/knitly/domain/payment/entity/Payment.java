package com.mysite.knitly.domain.payment.entity;

import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false, unique = true)
    private String tossPaymentKey; // 결제 고유 키

    @Column(nullable = false)
    private String tossOrderId; // 토스에서 관리하는 주문 번호

    @Column(length=100)
    private String mid; // 상점 id

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "order_id", nullable=false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User buyer; // 구매자 = Order.user

    @Column(nullable = false)
    private Long totalAmount; // 총 결제 금액

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod; // 결제 수단

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus; // 결제 상태

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt; // 결제 요청 시간

    @Column
    private LocalDateTime approvedAt; // 결제 승인 시간

    @Column
    private LocalDateTime canceledAt; // 결제 취소 시간

    @Column(length = 500)
    private String cancelReason; // 취소 사유 (취소 API에서 사용)


    // 결제 승인 처리
    public void approve(LocalDateTime approvedAt) {
        this.paymentStatus = PaymentStatus.DONE;
        this.approvedAt = approvedAt;
    }

    // 결제 취소 처리
    public void cancel(String cancelReason) {
        if (!this.isCancelable()) {
            throw new IllegalStateException("취소할 수 없는 결제 상태입니다: " + this.paymentStatus);
        }
        this.paymentStatus = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.cancelReason = cancelReason;
    }

    // 결제 실패 처리
    public void fail(String failureReason) {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    // 가상계좌 입금 대기 상태로 변경
    public void waitingForDeposit() {
        this.paymentStatus = PaymentStatus.WAITING_FOR_DEPOSIT;
    }

    // 결제 완료 여부 확인
    public boolean isCompleted() {
        return this.paymentStatus == PaymentStatus.DONE;
    }

    // 취소 가능 여부 확인
    public boolean isCancelable() {
        return this.paymentStatus.isCancelable();
    }
}
