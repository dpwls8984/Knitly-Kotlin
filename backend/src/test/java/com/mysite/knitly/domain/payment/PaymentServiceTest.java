package com.mysite.knitly.domain.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.domain.payment.client.TossApiClient;
import com.mysite.knitly.domain.payment.dto.PaymentCancelRequest;
import com.mysite.knitly.domain.payment.dto.PaymentCancelResponse;
import com.mysite.knitly.domain.payment.dto.PaymentConfirmRequest;
import com.mysite.knitly.domain.payment.dto.PaymentConfirmResponse;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import com.mysite.knitly.domain.payment.repository.PaymentRepository;
import com.mysite.knitly.domain.payment.service.PaymentService;
import com.mysite.knitly.domain.product.product.service.RedisProductService;
import com.mysite.knitly.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RedisProductService redisProductService;
    @Mock private TossApiClient tossApiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void init() {
    }

    @DisplayName("결제 승인 성공 시 READY → IN_PROGRESS → DONE 상태로 저장")
    @Test
    void confirmPayment_success_DONE() throws Exception {
        // given
        String orderId = "ORD-1";
        String paymentKey = "pay_1";
        long amount = 15000L;

        Order order = stubOrder(orderId, amount);
        Payment ready = stubPayment(order, PaymentStatus.READY, null);

        when(orderRepository.findByTossOrderId(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_OrderId(order.getOrderId())).thenReturn(Optional.of(ready));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JsonNode toss = objectMapper.readTree("""
          {
            "status":"DONE",
            "method":"CARD",
            "mId":"mid_x",
            "approvedAt":"2025-11-10T03:10:00Z",
            "orderName":"주문명",
            "totalAmount":15000,
            "card":{"company":"Hyundai","number":"1234-****","approveNo":"9999"}
          }
        """);
        when(tossApiClient.confirmPayment(any(PaymentConfirmRequest.class))).thenReturn(toss);

        PaymentConfirmRequest req = new PaymentConfirmRequest(paymentKey, orderId, amount);

        // when
        PaymentConfirmResponse res = paymentService.confirmPayment(req);

        // then
        assertEquals(PaymentStatus.DONE, res.status());
        assertEquals(paymentKey, res.paymentKey());
        assertEquals(orderId, res.orderId());
        // READY -> IN_PROGRESS 저장 + DONE 저장 최소 2회
        verify(paymentRepository, atLeast(2)).save(any(Payment.class));
    }

    @DisplayName("결제 조회 시 IN_PROGRESS → DONE으로 동기화")
    @Test
    void queryPaymentStatus_syncFromInProgressToDone() throws Exception {
        // given
        Payment payment = Payment.builder()
                .paymentId(1L)
                .tossPaymentKey("pay_q")
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JsonNode toss = objectMapper.readTree("""
          {"status":"DONE","method":"CARD","approvedAt":"2025-11-10T03:10:00Z"}
        """);
        when(tossApiClient.queryPayment("pay_q")).thenReturn(toss);

        // when
        PaymentStatus result = paymentService.queryPaymentStatus(1L);

        // then
        assertEquals(PaymentStatus.DONE, result);
        verify(paymentRepository).save(any(Payment.class));
    }

    @DisplayName("결제 취소 요청 시 CANCELED 상태로 업데이트")
    @Test
    void cancelPayment_success() throws Exception {
        // given
        Payment payment = Payment.builder()
                .paymentId(10L)
                .tossPaymentKey("pay_c")
                .paymentStatus(PaymentStatus.DONE)
                .totalAmount(10000L)
                .build();

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JsonNode tossCancel = objectMapper.readTree("""
                {"status":"CANCELED"}
                """);
        when(tossApiClient.cancelPayment(eq("pay_c"), anyString())).thenReturn(tossCancel);

        // when
        PaymentCancelResponse res = paymentService.cancelPayment(10L, new PaymentCancelRequest("사용자 취소"));

        // then
        assertEquals(PaymentStatus.CANCELED, res.status());
        verify(paymentRepository).save(any(Payment.class));
    }

    @DisplayName("웹훅 수신 시 Payment가 없으면 토스 API를 통해 동기화 후 저장")
    @Test
    void handleWebhook_syncWhenPaymentMissing() throws Exception {
        // given
        Map<String,Object> payload = new HashMap<>();
        payload.put("eventType","PAYMENT_APPROVED");
        Map<String,Object> data = new HashMap<>();
        data.put("paymentKey","pay_w");
        data.put("status","DONE");
        data.put("approvedAt","2025-11-10T03:10:00Z");
        payload.put("data", data);

        when(paymentRepository.findByTossPaymentKey("pay_w")).thenReturn(Optional.empty());

        // 토스 조회 → 동기화
        Order order = stubOrder("ORD-W", 5000L);
        when(orderRepository.findByTossOrderId("ORD-W")).thenReturn(Optional.of(order));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JsonNode toss = objectMapper.readTree("""
          { "paymentKey":"pay_w","status":"DONE","method":"CARD","orderId":"ORD-W","totalAmount":5000,
            "approvedAt":"2025-11-10T03:10:00Z" }
        """);
        when(tossApiClient.queryPayment("pay_w")).thenReturn(toss);

        // when
        paymentService.handleWebhook(payload);

        // then
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class)); // sync 저장
    }

    private Order stubOrder(String tossOrderId, long amount) {
        User buyer = User.builder()
                .userId(1L).build();

        Order order = Order.builder()
                .orderId(100L)
                .tossOrderId(tossOrderId)
                .user(buyer)
                .totalPrice((double) amount)
                .build();
        return order;
    }

    private Payment stubPayment(Order order, PaymentStatus status, String paymentKey) {
        Payment p = Payment.builder()
                .paymentId(200L)
                .order(order)
                .buyer(order.getUser())
                .paymentStatus(status)
                .tossOrderId(order.getTossOrderId())
                .tossPaymentKey(paymentKey)
                .totalAmount(order.getTotalPrice().longValue())
                .requestedAt(LocalDateTime.now().minusMinutes(15))
                .build();
        return p;
    }
}
