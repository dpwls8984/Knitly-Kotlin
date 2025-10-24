package com.mysite.knitly.domain.payment;

import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.domain.payment.dto.PaymentCancelRequest;
import com.mysite.knitly.domain.payment.dto.PaymentConfirmRequest;
import com.mysite.knitly.domain.payment.dto.PaymentDetailResponse;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentMethod;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import com.mysite.knitly.domain.payment.repository.PaymentRepository;
import com.mysite.knitly.domain.payment.service.PaymentService;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    @Mock
    PaymentRepository paymentRepository;

    @InjectMocks
    PaymentService paymentService;

    @Mock
    OrderRepository orderRepository;

    @Test
    @DisplayName("마이페이지 결제내역 단건 조회 - 성공(본인 주문)")
    void success() {
        User buyer = User.builder().userId(10L).name("홍길동").build();

        Payment payment = Payment.builder()
                .paymentId(100L)
                .buyer(buyer)
                .paymentMethod(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.DONE)
                .totalAmount(15000L)
                .requestedAt(LocalDateTime.now().minusMinutes(5))
                .approvedAt(LocalDateTime.now().minusMinutes(4))
                .tossPaymentKey("pk_123")
                .tossOrderId("order-1")
                .mid("MID001")
                .build();

        when(paymentRepository.findByOrder_OrderId(1L)).thenReturn(Optional.of(payment));

        PaymentDetailResponse res = paymentService.getPaymentDetailByOrder(buyer, 1L);

        assertThat(res.paymentId()).isEqualTo(100L);
        assertThat(res.buyerId()).isEqualTo(10L);
        assertThat(res.method()).isEqualTo(PaymentMethod.CARD);
        assertThat(res.status()).isEqualTo(PaymentStatus.DONE);
        verify(paymentRepository).findByOrder_OrderId(1L);
    }

    @Test
    @DisplayName("마이페이지 결제내역 단건 조회 - 결제 없음")
    void notFound() {
        User buyer = User.builder().userId(10L).name("홍길동").build();

        when(paymentRepository.findByOrder_OrderId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentDetailByOrder(buyer, 1L))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("마이페이지 결제내역 단건 조회 - 소유권 불일치(권한없음)")
    void unauthorized() {
        User buyer = User.builder().userId(10L).name("홍길동").build();
        User other = User.builder().userId(20L).name("김땡떙").build();

        Payment payment = Payment.builder()
                .paymentId(100L)
                .buyer(other) // 다른 사람 결제
                .paymentMethod(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.DONE)
                .totalAmount(1000L)
                .build();

        when(paymentRepository.findByOrder_OrderId(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPaymentDetailByOrder(buyer, 1L))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_UNAUTHORIZED_ACCESS); // 네 코드에 맞춰 사용
    }

    @Test
    @DisplayName("결제 승인 실패 - 주문 없음")
    void confirm_orderNotFound() {
        PaymentConfirmRequest req = new PaymentConfirmRequest("pk1", "1", 1000L);
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(req))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 승인 실패 - 금액 불일치")
    void confirm_amountMismatch() {
        PaymentConfirmRequest req = new PaymentConfirmRequest("pk1", "1", 2000L);
        Order order = Order.builder().orderId(1L).totalPrice(10000.0).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(req))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("결제 승인 실패 - 중복 결제")
    void confirm_duplicate() {
        PaymentConfirmRequest req = new PaymentConfirmRequest("pk1", "1", 1000L);
        Order order = Order.builder().orderId(1L).totalPrice(1000.0).build();
        Payment payment = Payment.builder().build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_OrderId(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(req))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("결제 취소 실패 - 결제 없음")
    void cancel_notFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.cancelPayment(99L, new PaymentCancelRequest("사유")))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 취소 실패 - 취소 불가 상태")
    void cancel_notCancelable() {
        Payment p = Payment.builder()
                .paymentId(1L)
                .paymentMethod(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.FAILED) // 취소 불가
                .totalAmount(1000L)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> paymentService.cancelPayment(1L, new PaymentCancelRequest("사유")))
                .isInstanceOf(ServiceException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_CANCELABLE);
    }
}
