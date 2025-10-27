package com.mysite.knitly.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysite.knitly.domain.order.entity.Order;
import com.mysite.knitly.domain.order.repository.OrderRepository;
import com.mysite.knitly.domain.payment.dto.*;
import com.mysite.knitly.domain.payment.entity.Payment;
import com.mysite.knitly.domain.payment.entity.PaymentMethod;
import com.mysite.knitly.domain.payment.entity.PaymentStatus;
import com.mysite.knitly.domain.payment.repository.PaymentRepository;
import com.mysite.knitly.domain.user.entity.User;
import com.mysite.knitly.global.exception.ErrorCode;
import com.mysite.knitly.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @Value("${payment.toss.secret-key}")
    private String tossSecretKey;

    private static final String TOSS_PAYMENT_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String TOSS_PAYMENT_CANCEL_URL = "https://api.tosspayments.com/v1/payments/%s/cancel";

    // 결제 승인
    @Transactional
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        // 1. 주문 정보 조회 및 검증 (tossOrderId로 조회)
        Order order = orderRepository.findByTossOrderId(request.orderId())
                .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 주문 금액 검증
        long orderAmount = order.getTotalPrice().longValue();
        if (orderAmount != request.amount()) {
            throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 3. 중복 결제 방지
        if (paymentRepository.findByOrder_OrderId(order.getOrderId()).isPresent()) {
            throw new ServiceException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        // 4. 토스페이먼츠 결제 승인 API 호출
        try {
            JsonNode tossResponse = callTossPaymentConfirmApi(request);

            // 5. Payment 엔티티 생성 및 저장
            Payment payment = createPaymentFromTossResponse(order, tossResponse);
            Payment savedPayment = paymentRepository.save(payment);

            // 6. 응답 데이터 생성
            PaymentConfirmResponse response = buildPaymentConfirmResponse(savedPayment, tossResponse);

            log.info("결제 승인 성공 - orderId: {}, paymentKey: {}, amount: {}",
                    request.orderId(), response.paymentKey(), response.totalAmount());

            return response;

        } catch (IOException e) {
            log.error("토스페이먼츠 API 호출 실패", e);
            throw new ServiceException(ErrorCode.PAYMENT_API_CALL_FAILED);
        } catch (Exception e) {
            log.error("결제 승인 처리 중 오류 발생", e);
            throw new ServiceException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    // 결제 취소
    @Transactional
    public PaymentCancelResponse cancelPayment(Long paymentId, PaymentCancelRequest request) {
        // 1. 결제 정보 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. 취소 가능 여부 확인
        if (!payment.isCancelable()) {
            throw new ServiceException(ErrorCode.PAYMENT_NOT_CANCELABLE);
        }

        // 3. 토스페이먼츠 결제 취소 API 호출
        try {
            callTossPaymentCancelApi(payment.getTossPaymentKey(), request.cancelReason());

            // 4. 취소 처리
            payment.cancel(request.cancelReason());
            paymentRepository.save(payment);

            // 5. 응답 생성
            PaymentCancelResponse response = PaymentCancelResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .paymentKey(payment.getTossPaymentKey())
                    .orderId(payment.getTossOrderId())
                    .status(payment.getPaymentStatus())
                    .cancelAmount(payment.getTotalAmount())
                    .cancelReason(request.cancelReason())
                    .canceledAt(payment.getCanceledAt())
                    .build();

            log.info("결제 취소 성공 - paymentId: {}, amount: {}", paymentId, payment.getTotalAmount());

            return response;

        } catch (IOException e) {
            log.error("토스페이먼츠 취소 API 호출 실패", e);
            throw new ServiceException(ErrorCode.PAYMENT_CANCEL_API_FAILED);
        }
    }

    // 토스페이먼츠 결제 승인 API 호출
    private JsonNode callTossPaymentConfirmApi(PaymentConfirmRequest request) throws IOException {
        String authorization = createBasicAuthHeader(tossSecretKey);
        String requestBody = objectMapper.writeValueAsString(request);

        URL url = new URL(TOSS_PAYMENT_CONFIRM_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", authorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        return handleTossApiResponse(connection);
    }

    // 토스페이먼츠 결제 취소 API 호출
    private JsonNode callTossPaymentCancelApi(String paymentKey, String cancelReason) throws IOException {
        String authorization = createBasicAuthHeader(tossSecretKey);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("cancelReason", cancelReason);
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);

        URL url = new URL(String.format(TOSS_PAYMENT_CANCEL_URL, paymentKey));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", authorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBodyJson.getBytes(StandardCharsets.UTF_8));
        }

        return handleTossApiResponse(connection);
    }

    // 토스 API 응답 처리
    private JsonNode handleTossApiResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        boolean isSuccess = responseCode == 200;

        try (InputStream is = isSuccess ? connection.getInputStream() : connection.getErrorStream()) {
            JsonNode responseJson = objectMapper.readTree(is);

            if (!isSuccess) {
                String errorCode = responseJson.has("code") ? responseJson.get("code").asText() : "UNKNOWN";
                String errorMessage = responseJson.has("message") ? responseJson.get("message").asText() : "API 호출 실패";
                log.error("토스페이먼츠 API 실패 - code: {}, message: {}", errorCode, errorMessage);
                throw new ServiceException(ErrorCode.PAYMENT_API_CALL_FAILED);
            }

            return responseJson;
        }
    }

    // Basic 인증 헤더 생성
    private String createBasicAuthHeader(String secretKey) {
        String credentials = secretKey + ":";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }

    // 토스 응답으로부터 Payment 엔티티 생성
    private Payment createPaymentFromTossResponse(Order order, JsonNode tossResponse) {
        String method = tossResponse.get("method").asText();
        String status = tossResponse.get("status").asText();

        Payment payment = Payment.builder()
                .tossPaymentKey(tossResponse.get("paymentKey").asText())
                .tossOrderId(tossResponse.get("orderId").asText())
                .mid(tossResponse.has("mId") ? tossResponse.get("mId").asText() : null)
                .order(order)
                .buyer(order.getUser())
                .totalAmount(tossResponse.get("totalAmount").asLong())
                .paymentMethod(PaymentMethod.fromString(method))
                .paymentStatus(PaymentStatus.fromString(status))
                .build();

        if (tossResponse.has("approvedAt")) {
            String approvedAtStr = tossResponse.get("approvedAt").asText();
            LocalDateTime approvedAt = LocalDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_DATE_TIME);
            payment.approve(approvedAt);
        }

        return payment;
    }

    // PaymentConfirmResponse 생성
    private PaymentConfirmResponse buildPaymentConfirmResponse(Payment payment, JsonNode tossResponse) {
        PaymentConfirmResponse.PaymentConfirmResponseBuilder builder = PaymentConfirmResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentKey(payment.getTossPaymentKey())
                .orderId(payment.getTossOrderId())
                .orderName(tossResponse.has("orderName") ? tossResponse.get("orderName").asText() : null)
                .method(payment.getPaymentMethod())
                .totalAmount(payment.getTotalAmount())
                .status(payment.getPaymentStatus())
                .requestedAt(payment.getRequestedAt())
                .approvedAt(payment.getApprovedAt())
                .mid(tossResponse.has("mId") ? tossResponse.get("mId").asText() : null);

        // 카드 결제 정보
        if (tossResponse.has("card")) {
            JsonNode card = tossResponse.get("card");
            builder.card(PaymentConfirmResponse.CardInfo.builder()
                    .company(card.has("company") ? card.get("company").asText() : null)
                    .number(card.has("number") ? card.get("number").asText() : null)
                    .installmentPlanMonths(card.has("installmentPlanMonths") ?
                            card.get("installmentPlanMonths").asText() : null)
                    .approveNo(card.has("approveNo") ? card.get("approveNo").asText() : null)
                    .ownerType(card.has("ownerType") ? card.get("ownerType").asText() : null)
                    .build());
        }

        // 가상계좌 정보
        if (tossResponse.has("virtualAccount")) {
            JsonNode va = tossResponse.get("virtualAccount");
            PaymentConfirmResponse.VirtualAccountInfo.VirtualAccountInfoBuilder vaBuilder =
                    PaymentConfirmResponse.VirtualAccountInfo.builder()
                            .accountNumber(va.has("accountNumber") ? va.get("accountNumber").asText() : null)
                            .bankCode(va.has("bankCode") ? va.get("bankCode").asText() : null)
                            .customerName(va.has("customerName") ? va.get("customerName").asText() : null);

            if (va.has("dueDate")) {
                String dueDateStr = va.get("dueDate").asText();
                vaBuilder.dueDate(LocalDateTime.parse(dueDateStr, DateTimeFormatter.ISO_DATE_TIME));
            }

            builder.virtualAccount(vaBuilder.build());
        }

        // 간편결제 정보
        if (tossResponse.has("easyPay")) {
            JsonNode easyPay = tossResponse.get("easyPay");
            builder.easyPay(PaymentConfirmResponse.EasyPayInfo.builder()
                    .provider(easyPay.has("provider") ? easyPay.get("provider").asText() : null)
                    .amount(easyPay.has("amount") ? easyPay.get("amount").asLong() : null)
                    .build());
        }

        return builder.build();
    }

    // 마이페이지에서 주문의 결제 내역 단건 조회

    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentDetailByOrder(User user, Long orderId) {
        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        // 본인 결제 내역인지 확인
        if (!payment.getBuyer().getUserId().equals(user.getUserId())) {
            throw new ServiceException(ErrorCode.PAYMENT_UNAUTHORIZED_ACCESS);
        }
        return PaymentDetailResponse.from(payment);
    }
}